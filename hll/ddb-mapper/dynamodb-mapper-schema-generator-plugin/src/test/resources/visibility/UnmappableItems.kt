/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.visibility

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype

public object Orders {
    @DynamoDbItem
    public data class Line(@DynamoDbPartitionKey var id: Int = 0, var sku: String = "")
}

public object Invoices {
    @DynamoDbItem
    public data class Line(@DynamoDbPartitionKey var id: Int = 0, var amount: Int = 0)
}

public class Outer {
    @DynamoDbItem
    public inner class Thing(@DynamoDbPartitionKey public var id: Int = 0)
}

@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "kind")
public sealed interface Catalog {
    @DynamoDbPartitionKey
    public val id: Int
}

@DynamoDbSubtype("entry")
public data class CatalogEntry(override val id: Int, val title: String) : Catalog

@DynamoDbHeterogeneousType(typeAttribute = "sub")
@DynamoDbSubtype("listings", parent = Catalog::class)
public object Listings : Catalog {
    override val id: Int = 0
}

@DynamoDbSubtype("listing", parent = Listings::class)
public data class Listing(@DynamoDbPartitionKey val id: Int, val price: Int)
