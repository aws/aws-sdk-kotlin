/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

/**
 * Identifies the type of key modeled by a given [Member]
 * @param keyTypeVar A [TypeVar] instance that may be used in codegen for this key type
 */
internal enum class MemberKeyType(val keyTypeVar: TypeVar) {
    /**
     * Represents a partition key
     */
    PARTITION(MapperTypes.Items.KeyTypeAsPK),

    /**
     * Represents a sort key
     */
    SORT(MapperTypes.Items.KeyTypeAsSK),
}

/**
 * Gets the [MemberKeyType] for this [Member], or `null` if the member does not model a key
 */
internal val Member.keyType: MemberKeyType?
    get() = attributes.getOrNull(MapperAttributes.MemberKeyType)
