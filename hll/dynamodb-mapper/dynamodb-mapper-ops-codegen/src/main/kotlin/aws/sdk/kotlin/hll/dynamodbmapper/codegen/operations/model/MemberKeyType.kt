/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

internal enum class MemberKeyType(val keyTypeVar: TypeVar) {
    PARTITION(MapperTypes.Items.KeyTypeAsPK),
    SORT(MapperTypes.Items.KeyTypeAsSK),
}

internal val Member.keyType: MemberKeyType?
    get() = attributes.getOrNull(MapperAttributes.MemberKeyType)
