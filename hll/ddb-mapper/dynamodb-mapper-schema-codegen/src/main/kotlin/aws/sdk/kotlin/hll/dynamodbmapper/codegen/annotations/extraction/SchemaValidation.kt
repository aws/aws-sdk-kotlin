/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbCounter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

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
