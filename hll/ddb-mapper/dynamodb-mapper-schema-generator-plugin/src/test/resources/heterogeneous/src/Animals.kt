/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtypeMatch

// An open concrete base which registers itself. With the default baseMatch (INSTANCE_OF), it catches every
// unregistered subclass.
@DynamoDbHeterogeneousType(typeAttribute = "kind", baseTypeName = "animal")
public open class Animal(public var name: String = "")

@DynamoDbSubtype("dog")
public open class Dog(public var breed: String = "") : Animal()

@DynamoDbSubtype("puppy", match = DynamoDbSubtypeMatch.EXACT_TYPE)
public class Puppy(public var ageWeeks: Int = 0) : Dog()

// Unregistered subclasses
public class Cat(public var indoor: Boolean = false) : Animal()
public class Poodle : Dog()
