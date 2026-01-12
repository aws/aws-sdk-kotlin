/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import kotlin.reflect.KClass

/**
 * Specifies the attribute name for a property in a [DynamoDbItem]-annotated class/interface. If this annotation is not
 * included then the attribute name matches the property name.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbAttribute(val name: String)

/**
 * Specifies the type of [ValueConverter] to be used when processing this attribute.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbAttributeConverter(val converter: KClass<out ValueConverter<*>>)

/**
 * Specifies that this class/interface describes an item type in a table. All public properties of this type will be mapped to
 * attributes unless they are explicitly ignored.
 * @param converter The item converter to be used for converting this class/interface. If not set, one will be automatically generated.
 */
@Target(AnnotationTarget.CLASS)
public annotation class DynamoDbItem(val converter: KClass<out ItemConverter<*>> = ItemConverter::class)

/**
 * Specifies that this property is the primary key for the item. Every top-level [DynamoDbItem] to be used in a table
 * must have exactly one partition key.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbPartitionKey

/**
 * Specifies that this property is the sort key for the item. Every top-level [DynamoDbItem] to be used in a table may
 * have at most one sort key.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbSortKey

/**
 * Specifies that this property should be ignored during mapping.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbIgnore

/**
 * Specifies that this property is used to track the item's time-to-live (TTL).
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html
 * @param lifetime The lifetime of this item, in seconds
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbTtlSeconds(val lifetime: Long)

/**
 * Specifies that this property should be used as a counter field, incrementing each time an item is persisted to DynamoDB.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbCounter
