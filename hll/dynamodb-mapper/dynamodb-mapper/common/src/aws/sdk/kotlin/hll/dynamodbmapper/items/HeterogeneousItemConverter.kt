/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * An item converter which handles heterogeneous (i.e., incongruent) data types by way of a string discriminator
 * attribute identified by [typeAttribute]. The given [typeMapper] function must return a string type name for an object
 * which will be used for the [typeAttribute] attribute. Finally, the given [subConverters] map identifies the delegate
 * converters for each type.
 *
 * This converter is particularly (although not _solely_) useful for mapping polymorphic structures. For example, given
 * a class hierarchy:
 *
 * ```kotlin
 * sealed interface Vehicle
 *
 * @DynamoDbItem
 * data class Car(
 *     @DynamoDbPartitionKey val id: Int,
 *     val manufacturer: String,
 *     val model: String,
 *     val year: Int,
 * ) : Vehicle
 *
 * @DynamoDbItem
 * data class Bike(
 *     @DynamoDbPartitionKey val id: Int,
 *     val manufacturer: String
 *     val gears: Int,
 *     val isElectric: Boolean,
 * ) : Vehicle
 * ```
 *
 * A heterogeneous item converter can be constructed:
 *
 * ```kotlin
 * fun vehicleType(obj: Vehicle) = when (obj) {
 *     is Car -> "car"
 *     is Bike -> "bike"
 * }
 *
 * val vehicleConverter = HeterogeneousItemConverter(
 *     typeMapper = ::vehicleType,
 *     typeAttribute = "type",
 *     subConverters = mapOf(
 *         "car" to CarConverter,
 *         "bike" to BikeConverter,
 *     ),
 * )
 * ```
 *
 * Objects mapped in this manner will use only the attributes relevant to their specific type, plus the [typeAttribute].
 * For example, given the following items and PutItem calls:
 *
 * ```kotlin
 * val vehicles = listOf(
 *     Car(1, "Ford", "Model T", 1928),
 *     Bike(2, "Schwinn", 10, false),
 *     Car(3, "Edsel", "Corsair", 1958),
 *     Bike(4, "Kuwahara", 1, false),
 * )
 *
 * val table = ... // some table which uses the vehicleConverter from above in its schema
 *
 * vehicles.forEach { vehicle ->
 *     table.putItem { item = vehicle }
 * }
 * ```
 *
 * Items would be persisted in the table as:
 *
 * | **id** | **type** | **manufacturer** | **model** | **year** | **gears** | **isElectric** |
 * |-------:|----------|------------------|-----------|---------:|----------:|----------------|
 * |      1 | car      | Ford             | Model T   |     1928 |           |                |
 * |      2 | bike     | Schwinn          |           |          |        10 | false          |
 * |      3 | car      | Edsel            | Corsair   |     1958 |           |                |
 * |      4 | bike     | Kuwahara         |           |          |         1 | false          |
 *
 * @param T The common type ancestor for all subtypes handled by this converter. This may be a base class, interface, or
 * even [Any].
 * @param typeMapper A function which accepts an instance of the common type [T] and returns the string identifier for
 * the type. This identifier is written/read from the attribute identified by [typeAttribute] and used as a lookup key
 * in [subConverters].
 * @param typeAttribute The name of the attribute in which to store/read type information. This attribute will be
 * present for every item persisted via this converter. It should ideally be an attribute which doesn't conflict with
 * other attributes used by subconverters.
 * @param subConverters A map of type names (the same returned by [typeMapper]) to [ItemConverter] instances. If the
 * [typeMapper] function returns a type name which does not exist in this map, or if an item is read containing a type
 * attribute value which does not exist in this map, an exception will be thrown.
 */
@ExperimentalApi
public class HeterogeneousItemConverter<T>(
    public val typeMapper: (T) -> String,
    public val typeAttribute: String,
    public val subConverters: Map<String, ItemConverter<T>>,
) : ItemConverter<T> {
    override val left: MonoConverter<Item, T> = MonoConverter { item ->
        val attr = item[typeAttribute] ?: error("Missing $typeAttribute")
        val typeValue = attr.asSOrNull() ?: error("No string value for $attr")
        val converter = subConverters[typeValue] ?: error("No converter for $typeValue")
        converter.convertLeft(item)
    }

    override val right: MonoConverter<T, Item> = MonoConverter { obj ->
        val typeValue = typeMapper(obj)
        val converter = subConverters[typeValue] ?: error("No converter for $typeValue")

        buildItem {
            put(typeAttribute, AttributeValue.S(typeValue))
            putAll(converter.convertRight(obj))
        }
    }
}
