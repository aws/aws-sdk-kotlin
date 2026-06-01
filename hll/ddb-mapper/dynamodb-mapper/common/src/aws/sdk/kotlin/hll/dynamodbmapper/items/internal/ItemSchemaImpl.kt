/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes

internal data class ItemSchemaPartitionKeyImpl<T, PK : KeyType>(
    override val converter: ItemConverter<T>,
    override val partitionKey: KeySpec<PK>,
    override val attributes: Attributes = emptyAttributes(),
) : ItemSchema.PartitionKey<T, PK>

internal data class ItemSchemaCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val converter: ItemConverter<T>,
    override val partitionKey: KeySpec<PK>,
    override val sortKey: KeySpec<SK>,
    override val attributes: Attributes = emptyAttributes(),
) : ItemSchema.CompositeKey<T, PK, SK>
