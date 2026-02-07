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
    val CodegenBehavior: AttributeKey<MemberCodegenBehavior> = AttributeKey("aws.sdk.kotlin.ddbmapper#CodegenBehavior")

    val TypeFamily: AttributeKey<TypeFamily> = AttributeKey("aws.sdk.kotlin#ddbmapper#TypeFamily")

    val IsInherited: AttributeKey<Boolean> = AttributeKey("aws.sdk.kotlin#InInherited")

    /**
     * Indicates whether this [Member] represents a reified key field derived from a low-level member with
     * [MemberCodegenBehavior.MapToKeys].
     */
    val MemberKeyType: AttributeKey<MemberKeyType> = AttributeKey("aws.sdk.kotlin.ddbmapper#MemberKeyType")

    val Variants: AttributeKey<List<Structure>> = AttributeKey("aws.sdk.kotlin.ddbmapper#Variants")

    val StructureKeyType: AttributeKey<StructureKeyType> = AttributeKey("aws.sdk.kotlin.ddbmapper#StructureKeyType")
}

internal val Member.isInherited: Boolean
    get() = attributes.getOrNull(MapperAttributes.IsInherited) ?: false
