/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model

import aws.sdk.kotlin.hll.codegen.model.TypeRef

/**
 * Table-level metadata for a [MappedItem]. Empty for nested-only items.
 * @param partitionKeys The partition key attributes, in declaration order
 * @param sortKeys The sort key attributes, in declaration order. Empty when there is no sort key.
 * @param ttlFields TTL lifetimes in seconds keyed by Kotlin property name, in declaration order
 * @param counterFields Kotlin property names of counter attributes, in declaration order
 */
internal data class SchemaMetadata(
    val partitionKeys: List<KeyAttribute>,
    val sortKeys: List<KeyAttribute>,
    val ttlFields: Map<String, Long>,
    val counterFields: Set<String>,
) {
    companion object {
        val Empty = SchemaMetadata(emptyList(), emptyList(), emptyMap(), emptySet())
    }
}

/**
 * A resolved key attribute
 * @param ddbName The DynamoDB attribute name
 * @param type The key property's type
 * @param keySpecType The key spec factory used when this is the first key (e.g., `KeySpec.string`)
 * @param thenType The key spec chaining function used when this follows another key (e.g., `thenString`)
 */
internal data class KeyAttribute(
    val ddbName: String,
    val type: TypeRef,
    val keySpecType: TypeRef,
    val thenType: TypeRef,
)
