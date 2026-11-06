/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isJavaPackagePrivate
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/*
 * Structural validation of annotated classes. Checks tied to resolving a type (unsupported attribute/key/map-key/set
 * element types, cross-module nesting) are detected where the type is resolved (ValueConverterResolver, KeyResolver),
 * and TTL/counter checks where those values are extracted, so each check has exactly one detection site.
 */

/**
 * `@DynamoDbMappable` targets `ANNOTATION_CLASS` so it can meta-annotate [DynamoDbItem], which means the Kotlin
 * compiler alone won't stop a user from incorrectly applying it to their own annotation class.
 * @return `true` if [decl] may be extracted further
 */
internal fun validateNotAnnotationClass(decl: KSClassDeclaration, diagnostics: DiagnosticCollector): Boolean {
    if (decl.classKind != ClassKind.ANNOTATION_CLASS) return true

    diagnostics.error(
        "@DynamoDbMappable cannot be applied to annotation classes " +
            "(${decl.qualifiedName?.asString()}); it is only valid on regular or data classes.",
        decl.asSymbolRef(),
    )
    return false
}

/**
 * Generated code lives in its own file (and usually its own package), so it can only reference a class which is
 * `public` or `internal`, as is every declaration enclosing it.
 * @return `true` if [decl] may be extracted further
 */
internal fun validateAccessibleFromGeneratedCode(decl: KSClassDeclaration, diagnostics: DiagnosticCollector): Boolean {
    val (inaccessible, visibility) = generateSequence<KSDeclaration>(decl) { it.parentDeclaration }
        .firstNotNullOfOrNull { declaration -> declaration.inaccessibleVisibility()?.let { declaration to it } }
        ?: return true

    val name = decl.qualifiedName?.asString() ?: decl.simpleName.asString()
    val reason = if (inaccessible === decl) "is $visibility" else "is nested in $visibility '${inaccessible.qualifiedName?.asString()}'"

    diagnostics.error(
        "'$name' $reason, so generated DynamoDbMapper code cannot access it. Annotated classes and every class " +
            "enclosing them must be public or internal.",
        decl.asSymbolRef(),
    )
    return false
}

/**
 * Generated code constructs annotated classes without an instance of any enclosing class, so an `inner` class can't be
 * mapped
 * @return `true` if [decl] may be extracted further
 */
internal fun validateNotInnerClass(decl: KSClassDeclaration, diagnostics: DiagnosticCollector): Boolean {
    if (Modifier.INNER !in decl.modifiers) return true

    diagnostics.error(
        "'${decl.qualifiedName?.asString()}' is an inner class, so generated DynamoDbMapper code cannot construct it. " +
            "Remove the `inner` modifier.",
        decl.asSymbolRef(),
    )
    return false
}

/**
 * Generated declarations are named after the innermost name of each annotated class, so two classes with the same name
 * whose code is generated into the same package (e.g., nested classes in different enclosing classes, or classes from
 * different packages with an absolute destination package) would generate conflicting declarations
 * @param isSelfRegisteringBase Whether a class is a family base which registers itself, which generates an additional
 * `<name>BaseConverter`
 * @return the qualified names of the colliding classes, each of which has been reported
 */
internal fun findGeneratedNameCollisions(
    classes: List<KSClassDeclaration>,
    ctx: RenderContext,
    isSelfRegisteringBase: (KSClassDeclaration) -> Boolean,
    diagnostics: DiagnosticCollector,
): Set<String> {
    val claimants = mutableMapOf<String, MutableSet<KSClassDeclaration>>()
    classes.forEach { decl ->
        val name = decl.simpleName.asString()
        val suffixes = listOf("Builder", "Converter", "ValueConverter", "Schema") +
            listOfNotNull("BaseConverter".takeIf { isSelfRegisteringBase(decl) })
        suffixes.forEach { suffix ->
            claimants.getOrPut("${destinationPackage(decl, ctx)}.$name$suffix") { linkedSetOf() } += decl
        }
    }

    val colliding = linkedSetOf<String>()
    claimants
        .filterValues { it.size > 1 }
        .forEach { (generatedName, decls) ->
            val names = decls.map { it.qualifiedName!!.asString() }
            decls.filter { colliding.add(it.qualifiedName!!.asString()) }.forEach { decl ->
                val others = names - decl.qualifiedName!!.asString()
                diagnostics.error(
                    "'${decl.qualifiedName!!.asString()}' and ${others.joinToString { "'$it'" }} would both generate " +
                        "'$generatedName'. Rename one of them, or generate their code into different packages.",
                    decl.asSymbolRef(),
                )
            }
        }
    return colliding
}

/**
 * Describes this declaration's visibility if code in another file and package of the same module cannot access it
 */
private fun KSDeclaration.inaccessibleVisibility(): String? = when {
    isPrivate() -> "private"
    isProtected() -> "protected"
    isJavaPackagePrivate() -> "package-private"
    else -> null
}

/**
 * Validates primary key requirements for a table item ([DynamoDbItem]): 1-4 partition key properties and 0-4 sort key
 * properties.
 */
internal fun validateTableItemKeys(
    decl: KSClassDeclaration,
    partitionKeyCount: Int,
    sortKeyCount: Int,
    diagnostics: DiagnosticCollector,
) {
    if (partitionKeyCount !in 1..4) {
        diagnostics.error(
            "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found $partitionKeyCount",
            decl.asSymbolRef(),
        )
    }
    if (sortKeyCount !in 0..4) {
        diagnostics.error(
            "Expected between 0 and 4 properties annotated with @DynamoDbSortKey, found $sortKeyCount",
            decl.asSymbolRef(),
        )
    }
}

/**
 * Validates a nested-only mappable type ([DynamoDbMappable] without [DynamoDbItem]). Key, TTL, and counter annotations
 * are meaningful only on table items, so their presence on a nested type is a compile-time error.
 */
@OptIn(KspExperimental::class)
internal fun validateNestedItem(
    className: String,
    properties: List<KSPropertyDeclaration>,
    diagnostics: DiagnosticCollector,
) {
    properties.forEach { prop ->
        val invalidAnnotations = buildList {
            if (prop.isPk) add("@DynamoDbPartitionKey")
            if (prop.isSk) add("@DynamoDbSortKey")
            if (prop.isAnnotationPresent(DynamoDbTtlSeconds::class)) add("@DynamoDbTtlSeconds")
            if (prop.isAnnotationPresent(DynamoDbCounter::class)) add("@DynamoDbCounter")
        }

        if (invalidAnnotations.isNotEmpty()) {
            diagnostics.error(
                "Property '${prop.name}' on nested type '$className' is annotated with " +
                    "${invalidAnnotations.joinToString()}. These annotations are only valid on a @DynamoDbItem " +
                    "table item, not on a nested @DynamoDbMappable type.",
                prop.asSymbolRef(),
            )
        }
    }
}
