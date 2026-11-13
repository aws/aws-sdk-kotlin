/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtypeMatch

@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
public sealed interface Vehicle {
    @DynamoDbPartitionKey
    public val id: Int
    public val manufacturer: String
}

// Unannotated intermediate: transparent to the family
public sealed interface TwoWheeler : Vehicle

@DynamoDbSubtype("car")
public data class Car(override val id: Int, override val manufacturer: String, val model: String) : Vehicle

@DynamoDbSubtype("bike")
public data class Bike(override val id: Int, override val manufacturer: String, val gears: Int) : TwoWheeler

@DynamoDbSubtype("unicycle")
public data object Unicycle : TwoWheeler {
    override val id: Int = 0
    override val manufacturer: String = "Generic"
}

// A nested family with its own type attribute, registered in Vehicle as a whole
@DynamoDbSubtype("truck", match = DynamoDbSubtypeMatch.INSTANCE_OF)
@DynamoDbHeterogeneousType(typeAttribute = "truckType")
public sealed class Truck : Vehicle

@DynamoDbSubtype("pickup")
public data class Pickup(override val id: Int, override val manufacturer: String, val bedLength: Int) : Truck()

@DynamoDbSubtype("flatbed")
public data class Flatbed(override val id: Int, override val manufacturer: String, val deckLength: Int) : Truck()

@DynamoDbItem
public data class Garage(
    @DynamoDbPartitionKey val name: String,
    val vehicles: List<Vehicle>,
    val favorite: Vehicle?,
)
