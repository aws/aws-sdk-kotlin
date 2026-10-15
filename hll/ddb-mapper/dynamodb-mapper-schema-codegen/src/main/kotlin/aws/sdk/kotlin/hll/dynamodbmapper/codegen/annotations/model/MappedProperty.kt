/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model

import aws.sdk.kotlin.hll.codegen.model.TypeRef

/**
 * A property of a [MappedItem] which maps to a DynamoDB attribute. Its value converter lives on
 * [ItemConverterSpec.Generated], since it is only resolved when the item converter is generated.
 * @param name The Kotlin property name
 * @param ddbName The DynamoDB attribute name (from `@DynamoDbAttribute`, defaulting to [name])
 * @param type The property's type
 */
internal data class MappedProperty(
    val name: String,
    val ddbName: String,
    val type: TypeRef,
)
