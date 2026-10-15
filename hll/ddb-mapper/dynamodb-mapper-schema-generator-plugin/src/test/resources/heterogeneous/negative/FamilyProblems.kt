/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbHeterogeneousType
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtype
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSubtypeMatch
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbTtlSeconds

// Each family below has exactly one independent problem; all of them must be reported together.

// A concrete sealed subtype that no registration covers
@DynamoDbHeterogeneousType(typeAttribute = "type")
public sealed interface Shape

@DynamoDbSubtype("circle")
public data class Circle(val radius: Int) : Shape

public data class Square(val side: Int) : Shape

// Members of a table family with different key types
@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
public sealed interface Doc

@DynamoDbSubtype("a")
public data class DocA(@DynamoDbPartitionKey val id: String) : Doc

@DynamoDbSubtype("b")
public data class DocB(@DynamoDbPartitionKey val id: Int) : Doc

// A property colliding with the type attribute
@DynamoDbHeterogeneousType(typeAttribute = "kind")
public sealed interface Pet

@DynamoDbSubtype("fish")
public data class Fish(val kind: String) : Pet

// A duplicate and a blank type name
@DynamoDbHeterogeneousType(typeAttribute = "type")
public sealed interface Fruit

@DynamoDbSubtype("pome")
public data class Apple(val a: Int) : Fruit

@DynamoDbSubtype("pome")
public data class Pear(val p: Int) : Fruit

@DynamoDbSubtype(" ")
public data class Plum(val p: Int) : Fruit

// EXACT_TYPE on an interface, which can never match
@DynamoDbHeterogeneousType(typeAttribute = "type")
public interface Tool

@DynamoDbSubtype("hammer", match = DynamoDbSubtypeMatch.EXACT_TYPE)
public interface Hammer : Tool

@DynamoDbSubtype("saw")
public class Saw(public var teeth: Int = 0) : Tool

// An ad-hoc family member without `parent`, leaving the family empty
@DynamoDbHeterogeneousType(typeAttribute = "type")
public object Things

@DynamoDbSubtype("thing")
public data class Thing(val t: Int)

// A heterogeneous attribute holding no value of the family's type
@DynamoDbMappable
public data class Holder(@DynamoDbHeterogeneousAttribute(Things::class) val n: Int)

// Conflicting TTL lifetimes across members
@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
public sealed interface Session

@DynamoDbSubtype("short")
public data class ShortSession(@DynamoDbPartitionKey val id: String, @DynamoDbTtlSeconds(60) val expiresAt: Long) : Session

@DynamoDbSubtype("long")
public data class LongSession(@DynamoDbPartitionKey val id: String, @DynamoDbTtlSeconds(120) val expiresAt: Long) : Session

// A final class can't have subtypes
@DynamoDbHeterogeneousType(typeAttribute = "type")
public class Sealed(public var x: Int = 0)
