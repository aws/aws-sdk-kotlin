/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSortKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.BuilderSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.GeneratedAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ItemConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.KeyAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedProperty
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.SchemaMetadata
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Builds the IR for a batch of annotated classes. Checks run in this order, preserving historical behavior:
 *  1. cycle detection over the whole batch; cyclic classes are reported and skipped before any per-class work
 *  2. per class, the annotation-class misuse check; a class failing it is not extracted further
 *  3. per class, key/nested validation, then converter, key, TTL and counter extraction
 *
 * Cross-class analysis (cycles, whether a value converter is needed) runs over *all* annotated classes, including ones
 * that later fail validation, exactly as before.
 * @param classes The annotated classes
 * @param ctx The base render context (logger, codegen factory, processor options)
 * @param diagnostics Receives every problem found
 * @return the IR for each class that passed extraction, in input order. If [diagnostics] has errors, callers must not
 * render any of it.
 */
internal fun buildModel(
    classes: List<KSClassDeclaration>,
    ctx: RenderContext,
    diagnostics: DiagnosticCollector,
): List<MappedItem> {
    val families = resolveFamilies(classes, diagnostics)
    val references = mappedTypeReferences(classes, families)

    // Cyclic nesting (including self-references) is not yet supported: the generated converters would form a
    // construction-time initialization cycle. Detect and reject such types with a compile-time error. A family's
    // converter is built from its members' converters, so a family also references each of its members.
    val cyclicClasses = findCyclicallyNestedClasses(
        references.mapValues { (name, refs) -> refs + families.families[name]?.members?.map { it.qualifiedName }.orEmpty() },
    )

    // Value converters must be generated for any mappable type which is referenced by another annotated type
    val referencedClasses = references.values.flatten().toSet()
    val familyBuilder = FamilyModelBuilder(families, ctx, diagnostics)

    return classes.flatMap { decl ->
        ctx.logger.info("Processing annotation on ${decl.simpleName}")

        val qualifiedName = decl.qualifiedName?.asString()
        if (qualifiedName != null && qualifiedName in cyclicClasses) {
            diagnostics.error(
                "Cyclic nesting detected involving type '$qualifiedName'. Nesting " +
                    "@DynamoDbItem/@DynamoDbMappable types in a reference cycle (including self-references) is " +
                    "not yet supported.",
                decl.asSymbolRef(),
            )
            return@flatMap emptyList()
        }

        val generateValueConverter = qualifiedName in referencedClasses
        val family = families.familyOf(decl)
        if (family != null) {
            familyBuilder.build(family, generateValueConverter)
        } else {
            val role = families.membershipOf(decl)?.let { FamilyRole(inTableFamily = families.inTableContext(it.family)) }
            listOfNotNull(buildMappedItem(decl, ctx, generateValueConverter, diagnostics, role))
        }
    }
}

/**
 * How a class takes part in a heterogeneous family, which changes how it is extracted
 * @param inTableFamily Whether the family is (transitively) a table family, which permits key, TTL and counter
 * annotations on its members
 * @param baseConverter Set only for a self-registering base: the name of the converter for the base's own instances.
 * Such an item is always nested-only and is generated into its own file.
 */
internal data class FamilyRole(
    val inTableFamily: Boolean,
    val baseConverter: TypeRef? = null,
)

/**
 * Builds the IR for a single annotated class
 * @param decl The annotated class
 * @param ctx The base render context
 * @param generateValueConverter Whether another mapped type references this one (computed across the batch)
 * @return the IR, or `null` if extraction reported any error for this class
 */
@OptIn(KspExperimental::class)
internal fun buildMappedItem(
    decl: KSClassDeclaration,
    ctx: RenderContext,
    generateValueConverter: Boolean,
    diagnostics: DiagnosticCollector,
    role: FamilyRole? = null,
): MappedItem? {
    if (!validateNotAnnotationClass(decl, diagnostics)) return null

    val errorsBefore = diagnostics.errorCount
    val name = decl.qualifiedName!!.getShortName()
    val isObject = decl.classKind == ClassKind.OBJECT
    val isSelfRegisteredBase = role?.baseConverter != null

    val kind = if (decl.isAnnotationPresent(DynamoDbItem::class) && !isSelfRegisteredBase) MappedItem.Kind.Table else MappedItem.Kind.Nested
    val type = Type.from(decl)

    // Family members inherit annotations from overridden base properties (e.g., a partition key declared on the base)
    val properties = if (role != null) {
        decl.getSourceProperties(inherit = true)
    } else {
        decl.getMappedProperties().map { SourceProperty(it, it.toMappedProperty()) }
    }

    val partitionKeyProps = properties.filter { it.isPk }
    val sortKeyProps = properties.filter { it.isSk }

    when (kind) {
        MappedItem.Kind.Table -> validateTableItemKeys(decl, partitionKeyProps.size, sortKeyProps.size, diagnostics)

        // Members of a table family may carry key, TTL and counter annotations; the family validates and merges them
        MappedItem.Kind.Nested -> if (role?.inTableFamily != true) {
            validateNestedItem(name, properties.map { it.decl }, diagnostics)
        }
    }

    val builder = if (!isObject && shouldRenderBuilder(decl, properties, ctx)) {
        BuilderSpec(
            builtStructure = Structure.from(decl),
            implementationType = type,
            members = properties.map { Member.from(it.decl) }.toSet(),
        )
    } else {
        null
    }

    val itemConverter = when (val specified = decl.specifiedItemConverter(kind)) {
        null -> {
            val attributes = properties.mapNotNull { prop ->
                resolvePropertyConverter(prop.decl, ctx, diagnostics, prop.annotations)?.let { GeneratedAttribute(prop.mapped, it) }
            }
            ItemConverterSpec.Generated(role?.baseConverter ?: generatedItemConverter(decl, ctx), attributes)
        }

        // A user-provided item converter owns its attributes' conversion, so property types are never resolved
        else -> ItemConverterSpec.UserProvided(Type.from(specified))
    }

    val metadata = when (kind) {
        MappedItem.Kind.Table -> SchemaMetadata(
            partitionKeys = resolveKeys(partitionKeyProps, diagnostics),
            sortKeys = resolveKeys(sortKeyProps, diagnostics),
            ttlFields = ttlFields(properties, diagnostics),
            counterFields = counterFields(properties, diagnostics),
        )
        MappedItem.Kind.Nested -> SchemaMetadata.Empty
    }

    if (diagnostics.errorCount > errorsBefore) return null

    return MappedItem(
        name = name,
        type = type,
        kind = kind,
        itemConverter = itemConverter,
        builder = builder,
        valueConverter = if (generateValueConverter && !isSelfRegisteredBase) generatedValueConverter(decl, ctx) else null,
        metadata = metadata,
        generateGetTable = kind == MappedItem.Kind.Table && ctx.attributes[AnnotationsProcessorOptions.GenerateGetTableMethodAttribute],
        destinationPkg = destinationPackage(decl, ctx),
        isObject = isObject,
        fileName = role?.baseConverter?.shortName ?: "${name}Schema",
    )
}

/**
 * A mapped property in both its KSP and IR forms, so extraction steps read each KSP fact only once
 * @param annotations The property's effective annotations, which for family members include those of overridden
 * properties
 */
internal data class SourceProperty(
    val decl: KSPropertyDeclaration,
    val mapped: MappedProperty,
    val annotations: List<KSAnnotation> = decl.annotations.toList(),
) {
    val isPk: Boolean get() = annotations.hasAnnotation(DynamoDbPartitionKey::class.qualifiedName!!)
    val isSk: Boolean get() = annotations.hasAnnotation(DynamoDbSortKey::class.qualifiedName!!)
}

internal fun resolveKeys(keyProps: List<SourceProperty>, diagnostics: DiagnosticCollector): List<KeyAttribute> = keyProps.mapNotNull { resolveKeyAttribute(it.mapped, it.decl.asSymbolRef(), diagnostics) }

/**
 * Skip rendering a class builder if:
 *   - the user has configured GenerateBuilders to WHEN_REQUIRED (default value) AND
 *   - the class has all mutable members AND
 *   - the class has a zero-arg constructor
 */
private fun shouldRenderBuilder(decl: KSClassDeclaration, properties: List<SourceProperty>, ctx: RenderContext): Boolean {
    val alwaysGenerateBuilders = ctx.attributes[AnnotationsProcessorOptions.GenerateBuilderClassesAttribute] == GenerateBuilderClasses.ALWAYS
    val hasAllMutableMembers = properties.all { it.decl.isMutable }
    val hasZeroArgConstructor = decl.getConstructors().any { constructor -> constructor.parameters.all { it.hasDefault } }

    return !(!alwaysGenerateBuilders && hasAllMutableMembers && hasZeroArgConstructor)
}

/**
 * Reads the user-specified item converter (if any) from whichever marker annotation applies to this type, or from
 * [DynamoDbSubtype]
 */
private fun KSClassDeclaration.specifiedItemConverter(kind: MappedItem.Kind): KSType? {
    val markerQualifiedName = when (kind) {
        MappedItem.Kind.Table -> DynamoDbItem::class.qualifiedName!!
        MappedItem.Kind.Nested -> DynamoDbMappable::class.qualifiedName!!
    }

    return annotationNamed(markerQualifiedName)?.converterArgument()
        ?: annotationNamed(DynamoDbSubtype::class.qualifiedName!!)?.converterArgument()
}

/**
 * TTL lifetimes keyed by Kotlin property name, in declaration order. Invalid lifetimes are reported and omitted.
 */
internal fun ttlFields(properties: List<SourceProperty>, diagnostics: DiagnosticCollector): Map<String, Long> = properties.mapNotNull { sp ->
    val (decl, prop) = sp
    val annotation = sp.annotations
        .singleOrNull { it.qualifiedName == DynamoDbTtlSeconds::class.qualifiedName }
        ?: return@mapNotNull null

    val lifetime = annotation.arguments.single().value as? Long
    when {
        lifetime == null -> diagnostics.error(
            "@DynamoDbTtlSeconds annotation argument on property ${prop.ddbName} could not be evaluated at compile time. Use a literal value like @DynamoDbTtlSeconds(3600) instead of expressions like @DynamoDbTtlSeconds(1.hours.inWholeSeconds).",
            decl.asSymbolRef(),
        )
        lifetime <= 0 -> diagnostics.error(
            "@DynamoDbTtlSeconds must be positive, got $lifetime seconds on property ${prop.ddbName}",
            decl.asSymbolRef(),
        )
        else -> return@mapNotNull prop.name to lifetime
    }
    null
}.toMap()

/**
 * Kotlin property names of counter attributes, in declaration order. Counters of an invalid type are reported and
 * omitted.
 */
internal fun counterFields(properties: List<SourceProperty>, diagnostics: DiagnosticCollector): Set<String> = properties.mapNotNull { sp ->
    val (decl, prop) = sp
    sp.annotations
        .singleOrNull { it.qualifiedName == DynamoDbCounter::class.qualifiedName }
        ?: return@mapNotNull null

    if (prop.type != Types.Kotlin.Int && prop.type != Types.Kotlin.Long) {
        diagnostics.error(
            "Property '${prop.name}' annotated with @DynamoDbCounter must be of type Int or Long, but was ${prop.type.shortName}",
            decl.asSymbolRef(),
        )
        return@mapNotNull null
    }
    prop.name
}.toSet()

/**
 * For each annotated class (by qualified name), the annotated classes its mapped properties reference, at any depth of
 * type arguments (e.g. `List<Map<String, Nested>>`). Computed once per batch, resolving each property type once.
 */
private fun mappedTypeReferences(classes: List<KSClassDeclaration>, families: FamilyIndex): Map<String, Set<String>> {
    val classNames = classes.mapTo(mutableSetOf()) { requireNotNull(it.qualifiedName).asString() }
    return classes.associate { decl ->
        val inherit = families.membershipOf(decl) != null || families.familyOf(decl) != null
        val properties = if (inherit) {
            decl.getSourceProperties(inherit = true).map { it.decl to it.annotations }
        } else {
            decl.getMappedProperties().map { it to it.annotations.toList() }
        }

        requireNotNull(decl.qualifiedName).asString() to properties
            .flatMap { (prop, annotations) ->
                // A @DynamoDbHeterogeneousAttribute property references its family even though its type (Any) doesn't
                val family = heterogeneousAttributeFamily(annotations)?.qualifiedName?.asString()
                prop.type.resolve().referencedTypeNames() + listOfNotNull(family)
            }
            .filterTo(mutableSetOf()) { it in classNames }
    }
}

/**
 * Returns the qualified names of annotated classes that participate in a nesting reference cycle (a type that can
 * transitively reference itself through its mappable properties, including direct self-references).
 * @param adjacency References between annotated classes, from [mappedTypeReferences]
 */
private fun findCyclicallyNestedClasses(adjacency: Map<String, Set<String>>): Set<String> = adjacency.keys.filterTo(mutableSetOf()) { start ->
    // A class is cyclic if it can reach itself by following references.
    val visited = mutableSetOf<String>()
    val toVisit = ArrayDeque(adjacency[start].orEmpty())
    var reachesSelf = false
    while (toVisit.isNotEmpty()) {
        val current = toVisit.removeLast()
        if (current == start) {
            reachesSelf = true
            break
        }
        if (visited.add(current)) {
            toVisit.addAll(adjacency[current].orEmpty())
        }
    }
    reachesSelf
}

/** Yields the qualified names of this type and all of its type arguments, recursively. */
private fun KSType.referencedTypeNames(): Sequence<String> = sequence {
    declaration.qualifiedName?.asString()?.let { yield(it) }
    arguments.forEach { arg -> arg.type?.resolve()?.let { yieldAll(it.referencedTypeNames()) } }
}
