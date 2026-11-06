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

public object PublicContainer {
    // No builder: all members are mutable and there is a zero-arg constructor
    @DynamoDbItem
    public data class Widget(@DynamoDbPartitionKey var id: Int = 0, var name: String = "")
}

internal object InternalContainer {
    // A builder: the members are immutable
    @DynamoDbItem
    public data class Gadget(@DynamoDbPartitionKey val id: Int, val name: String)
}

public object Container {
    // A nested mappable used as another item's property type
    @DynamoDbMappable
    public data class Address(val street: String, val zip: Int)

    // A nested object item
    @DynamoDbItem
    public object Settings {
        @DynamoDbPartitionKey
        public val id: Int = 1
        public val theme: String = "dark"
    }
}

@DynamoDbItem
public data class Person(@DynamoDbPartitionKey val id: Int, val home: Container.Address, val badge: Person.Badge) {
    @DynamoDbMappable
    public data class Badge(val label: String)
}

public object Zoo {
    // A nested self-registering family base
    @DynamoDbItem
    @DynamoDbHeterogeneousType(typeAttribute = "kind", baseTypeName = "animal")
    public open class Animal(@DynamoDbPartitionKey public open val id: Int = 0, public open val name: String = "")

    @DynamoDbSubtype("dog")
    public class Dog(override val id: Int = 0, override val name: String = "", public val barks: Int = 0) : Animal(id, name)
}

@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
public sealed interface Shape {
    @DynamoDbPartitionKey
    public val id: Int

    @DynamoDbSubtype("circle")
    public data class Circle(override val id: Int, val radius: Int) : Shape

    @DynamoDbSubtype("square")
    public data class Square(override val id: Int, val side: Int) : Shape
}
