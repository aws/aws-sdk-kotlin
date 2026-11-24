/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.visibility

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter

@DynamoDbMappable
internal data class Detail(var note: String = "")

// No builder: the internal type is referenced only inside the converter's initializer
@DynamoDbItem
public class WithoutBuilder {
    @DynamoDbPartitionKey
    public var id: Int = 0
    internal var detail: Detail = Detail()
}

// The builder's `detail` property exposes the internal type
@DynamoDbItem
public data class WithBuilder internal constructor(
    @DynamoDbPartitionKey val id: Int,
    internal val detail: Detail,
)

// The schema's `converter` property exposes the internal converter type
@DynamoDbItem(converter = InternalConverter::class)
public data class WithInternalConverter(@DynamoDbPartitionKey var id: Int = 0)

internal object InternalConverter : ItemConverter<WithInternalConverter> by SimpleItemConverter(
    builderFactory = { WithInternalConverter() },
    build = { this },
    descriptors = arrayOf(
        AttributeDescriptor("id", WithInternalConverter::id, WithInternalConverter::id::set, NumberValueConverters.Int),
    ),
)

// A nested-only item names its user-provided converter only inside its value converter's initializer
@DynamoDbMappable(converter = InternalAddressConverter::class)
public data class Address(var street: String = "")

internal object InternalAddressConverter : ItemConverter<Address> by SimpleItemConverter(
    builderFactory = { Address() },
    build = { this },
    descriptors = arrayOf(
        AttributeDescriptor("street", Address::street, Address::street::set, StringValueConverter),
    ),
)

@DynamoDbItem
public data class Customer(@DynamoDbPartitionKey var id: Int = 0, var address: Address = Address())

// A self-registering base whose builder exposes an internal type, in a family whose signatures name only the base
@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "kind", baseTypeName = "base")
public open class Thing internal constructor(@DynamoDbPartitionKey public open val id: Int, internal open val detail: Detail)

@DynamoDbSubtype("sub")
public class SubThing internal constructor(override val id: Int, override val detail: Detail, public val extra: Int) : Thing(id, detail)
