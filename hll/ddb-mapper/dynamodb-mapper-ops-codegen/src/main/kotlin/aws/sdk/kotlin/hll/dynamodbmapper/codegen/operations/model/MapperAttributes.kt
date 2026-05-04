/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Defines [AttributeKey] instances that relate to the data model of low-level to high-level codegen
 */
internal object MapperAttributes {
    /**
     * Gets the [MemberCodegenBehavior] associated with this [Member]
     */
    val CodegenBehavior: AttributeKey<MemberCodegenBehavior> = AttributeKey("aws.sdk.kotlin.ddbmapper#CodegenBehavior")

    /**
     * Gets the list of additional parameters used in the high-low conversion method generated for a given structure
     */
    val ConversionParameters: AttributeKey<List<ConversionParameter>> = AttributeKey("aws.sdk.kotlin.ddbmapper#ConversionParameters")

    /**
     * Identifies whether this [Member] is inherited from the type hierarchy of its [Structure]
     */
    val IsInherited: AttributeKey<Boolean> = AttributeKey("aws.sdk.kotlin#InInherited")

    /**
     * Gets the [aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.KeyProjections] associated with this
     * [Structure]
     */
    val KeyProjections: AttributeKey<KeyProjections> = AttributeKey("aws.sdk.kotlin.ddbmapper#KeyProjections")

    /**
     * Gets the [aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.KeyProjectionType] associated with this
     * [Structure]
     */
    val KeyProjectionType: AttributeKey<KeyProjectionType> = AttributeKey("aws.sdk.kotlin.ddbmapper#KeyProjectionType")

    /**
     * Indicates whether this [Member] represents a reified key field derived from a low-level member with
     * [MemberCodegenBehavior.MapToKeys].
     */
    val MemberKeyType: AttributeKey<MemberKeyType> = AttributeKey("aws.sdk.kotlin.ddbmapper#MemberKeyType")
}

/**
 * Identifies whether this [Member] is inherited from the type hierarchy of its [Structure]
 */
internal val Member.isInherited: Boolean
    get() = attributes.getOrNull(MapperAttributes.IsInherited) ?: false
