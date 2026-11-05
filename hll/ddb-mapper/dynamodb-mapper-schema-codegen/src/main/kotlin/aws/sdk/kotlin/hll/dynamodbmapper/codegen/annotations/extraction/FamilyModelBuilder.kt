/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.ItemConverterSpec
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.KeyAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.SchemaMetadata
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.SubtypeRegistration
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.symbol.KSClassDeclaration

private val itemName = DynamoDbItem::class.qualifiedName!!
private val mappableName = DynamoDbMappable::class.qualifiedName!!

/**
 * The value type of an ad-hoc family
 */
internal val AnyType = TypeRef("kotlin", "Any")

/**
 * Builds the IR for heterogeneous families. Table metadata is merged across members once per family and cached, so a
 * nested family's problems are reported once even though its outer family also reads its metadata.
 */
internal class FamilyModelBuilder(
    private val families: FamilyIndex,
    private val ctx: RenderContext,
    private val diagnostics: DiagnosticCollector,
) {
    private val metadataCache = mutableMapOf<String, SchemaMetadata?>()

    /**
     * Builds the family's own item and, for a self-registering base, the item for the base's own instances
     * @return the items, or an empty list if any error was reported for this family
     */
    fun build(family: Family, generateValueConverter: Boolean): List<MappedItem> {
        val errorsBefore = diagnostics.errorCount
        val base = family.base
        val name = base.qualifiedName!!.getShortName()
        val inTableContext = families.inTableContext(family)
        val valueType = if (family.isAdHoc) AnyType else Type.from(base)

        // In a nested-only family, annotations on the base's own properties are as invalid as on its members
        if (!inTableContext && !family.isAdHoc) {
            validateNestedItem(name, base.getMappedProperties(), diagnostics)
        }

        val baseConverter = family.baseTypeName?.let { TypeRef(destinationPackage(base, ctx), "${name}BaseConverter") }
        val baseItem = baseConverter?.let {
            buildMappedItem(base, ctx, generateValueConverter = false, diagnostics, FamilyRole(inTableContext, baseConverter = it))
        }

        val memberRegistrations = orderRegistrations(family.members, { it.qualifiedName }, { it.decl.ancestorNames() })
            .map { member -> SubtypeRegistration(member.typeName, Type.from(member.decl), member.exact, converterFor(member)) }

        // The base matches every member, so it must be registered last
        val baseRegistration = baseConverter?.let { SubtypeRegistration(family.baseTypeName!!, valueType, family.baseExact, it) }

        val metadata = if (family.isTable) familyMetadata(family) ?: SchemaMetadata.Empty else SchemaMetadata.Empty

        if (diagnostics.errorCount > errorsBefore) return emptyList()

        val familyItem = MappedItem(
            name = name,
            type = valueType,
            kind = if (family.isTable) MappedItem.Kind.Table else MappedItem.Kind.Nested,
            itemConverter = ItemConverterSpec.Heterogeneous(
                converter = generatedItemConverter(base, ctx),
                typeAttribute = family.typeAttribute,
                registrations = memberRegistrations + listOfNotNull(baseRegistration),
            ),
            builder = null,
            valueConverter = if (generateValueConverter) generatedValueConverter(base, ctx) else null,
            metadata = metadata,
            generateGetTable = family.isTable && ctx.attributes[AnnotationsProcessorOptions.GenerateGetTableMethodAttribute],
            destinationPkg = destinationPackage(base, ctx),
        )

        return listOfNotNull(familyItem, baseItem)
    }

    /**
     * The converter a family registers for [member]: its user-provided converter if it has one, otherwise the one
     * generated for it (which, for a nested family, is that family's heterogeneous converter)
     */
    private fun converterFor(member: FamilyMember): TypeRef {
        val specified = member.customConverter
            ?: member.decl.annotationNamed(itemName)?.converterArgument()
            ?: member.decl.annotationNamed(mappableName)?.converterArgument()
        return specified?.let(Type::from) ?: generatedItemConverter(member.decl, ctx)
    }

    /**
     * The table metadata shared by every member of [family], or `null` if it couldn't be determined (in which case an
     * error was reported)
     */
    private fun familyMetadata(family: Family): SchemaMetadata? {
        if (family.qualifiedName !in metadataCache) {
            metadataCache[family.qualifiedName] = mergeMetadata(family)
        }
        return metadataCache[family.qualifiedName]
    }

    private fun mergeMetadata(family: Family): SchemaMetadata? {
        val contributions = family.members.map { member ->
            member.decl to (families.familyOf(member.decl)?.let(::familyMetadata) ?: memberMetadata(member.decl))
        } + listOfNotNull(family.baseTypeName?.let { family.base to memberMetadata(family.base) })

        // A member whose metadata couldn't be extracted has already been reported
        if (contributions.isEmpty() || contributions.any { it.second == null }) return null

        val errorsBefore = diagnostics.errorCount
        val (referenceDecl, reference) = contributions.first()
        val referenceName = referenceDecl.qualifiedName?.asString()

        contributions.forEach { (decl, contribution) ->
            contribution!!
            val name = decl.qualifiedName?.asString()
            if (contribution.partitionKeys != reference!!.partitionKeys || contribution.sortKeys != reference.sortKeys) {
                diagnostics.error(
                    "Subtype '$name' has keys ${describeKeys(contribution)} but '$referenceName' has keys " +
                        "${describeKeys(reference)}. All members of table family '${family.qualifiedName}' must " +
                        "have the same key attribute names, types and order.",
                    decl.asSymbolRef(),
                )
            }

            // The TTL and counter interceptors apply a schema's fields to every item written with it, so a field
            // declared by only some members would be written into (and, for TTL, expire) items of the others
            if (contribution.ttlFields != reference.ttlFields) {
                diagnostics.error(
                    "Subtype '$name' has TTL attributes ${describeTtls(contribution)} but '$referenceName' has TTL " +
                        "attributes ${describeTtls(reference)}. All members of table family " +
                        "'${family.qualifiedName}' must declare the same @DynamoDbTtlSeconds attributes with the " +
                        "same lifetimes.",
                    decl.asSymbolRef(),
                )
            }

            if (contribution.counterFields != reference.counterFields) {
                diagnostics.error(
                    "Subtype '$name' has counter attributes ${describeCounters(contribution)} but '$referenceName' " +
                        "has counter attributes ${describeCounters(reference)}. All members of table family " +
                        "'${family.qualifiedName}' must declare the same @DynamoDbCounter attributes.",
                    decl.asSymbolRef(),
                )
            }
        }

        return if (diagnostics.errorCount > errorsBefore) null else reference
    }

    /**
     * Extracts the table metadata of a single (non-family) member. A member which is itself a `@DynamoDbItem` is
     * extracted (and its problems reported) as its own table item too, so its problems aren't reported again here.
     */
    private fun memberMetadata(decl: KSClassDeclaration): SchemaMetadata? {
        val reportsOwnProblems = decl.annotationNamed(itemName) != null && families.familyOf(decl) == null
        val memberDiagnostics = if (reportsOwnProblems) DiagnosticCollector() else diagnostics
        val errorsBefore = memberDiagnostics.errorCount

        val properties = decl.getSourceProperties(inherit = true)
        val partitionKeyProps = properties.filter { it.isPk }
        val sortKeyProps = properties.filter { it.isSk }
        validateTableItemKeys(decl, partitionKeyProps.size, sortKeyProps.size, memberDiagnostics)

        val metadata = SchemaMetadata(
            partitionKeys = resolveKeys(partitionKeyProps, memberDiagnostics),
            sortKeys = resolveKeys(sortKeyProps, memberDiagnostics),
            ttlFields = ttlFields(properties, memberDiagnostics),
            counterFields = counterFields(properties, memberDiagnostics),
        )
        return metadata.takeIf { memberDiagnostics.errorCount == errorsBefore }
    }
}

private fun describeKeys(metadata: SchemaMetadata): String {
    fun List<KeyAttribute>.describe() = joinToString(prefix = "[", postfix = "]") { "${it.ddbName}: ${it.type.shortName}" }
    return "(partition ${metadata.partitionKeys.describe()}, sort ${metadata.sortKeys.describe()})"
}

private fun describeTtls(metadata: SchemaMetadata): String = metadata.ttlFields.entries.sortedBy { it.key }.joinToString(prefix = "[", postfix = "]") { "${it.key}: ${it.value}s" }

private fun describeCounters(metadata: SchemaMetadata): String = metadata.counterFields.sorted().joinToString(prefix = "[", postfix = "]")
