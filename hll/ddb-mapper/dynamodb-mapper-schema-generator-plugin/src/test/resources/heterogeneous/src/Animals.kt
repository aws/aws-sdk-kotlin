/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtypeMatch

// An open concrete base which registers itself. With baseMatch = INSTANCE_OF, it catches every unregistered subclass.
@DynamoDbHeterogeneousType(typeAttribute = "kind", baseTypeName = "animal", baseMatch = DynamoDbSubtypeMatch.INSTANCE_OF)
public open class Animal(public var name: String = "")

@DynamoDbSubtype("dog", match = DynamoDbSubtypeMatch.INSTANCE_OF)
public open class Dog(public var breed: String = "") : Animal()

@DynamoDbSubtype("puppy")
public class Puppy(public var ageWeeks: Int = 0) : Dog()

// Unregistered subclasses
public class Cat(public var indoor: Boolean = false) : Animal()
public class Poodle : Dog()

// A self-registering base with the default baseMatch (EXACT_TYPE), which doesn't catch unregistered subclasses
@DynamoDbHeterogeneousType(typeAttribute = "kind", baseTypeName = "plant")
public open class Plant(public var name: String = "")

@DynamoDbSubtype("tree")
public class Tree(public var height: Int = 0) : Plant()

// Unregistered subclass
public class Shrub : Plant()
