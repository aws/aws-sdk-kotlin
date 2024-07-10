/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * An item converter which uses attribute descriptors to convert objects to items and vice versa. This converter
 * distinguishes between the object type used for conversion into an item (treated as immutable) and a mutable builder
 * type used for conversion from an item. Note that these do not have to be different types if the object type is
 * already mutable.
 * @param T The type of objects which will be converted to items. This converter treats type [T] as immutable.
 * @param B The type of builders which will be used when converting from items.
 * @param builderFactory A function which returns a new, default-valued builder object. This is invoked once for every
 * item which must be converted to an object.
 * @param build A method which builds a builder object of type [B] into its final representation of type [T]. If [B] and
 * [T] are the same type, this may be an identity function.
 * @param descriptors A collection of [AttributeDescriptor] which describe how to construct and parse attributes
 */
public class SimpleItemConverter<T, B>(
    private val builderFactory: () -> B,
    private val build: B.() -> T,
    vararg descriptors: AttributeDescriptor<*, T, B>,
) : ItemConverter<T> {
    private val descriptors = descriptors
        .groupBy { it.name }
        .mapValues { (name, descriptor) ->
            requireNotNull(descriptor.singleOrNull()) {
                """Multiple AttributeDescriptor instances for attribute "$name""""
            }
        }

    override fun fromItem(item: Item): T {
        val builder = builderFactory()

        /**
         * This is a convenience function to keep the compile-time safety for type param `A`. Without this, the compiler
         * can't track generic types across multiple statements:
         *
         * ```kotlin
         * val descriptor = descriptors[name] // AttributeDescriptor<*, T, B>
         * val value = descriptor.converter.fromAv(av) // Any?
         * descriptor.setter(builder, value) // Type mismatch for value. Required: Nothing, Found: Any?
         * ```
         */
        fun <A> AttributeDescriptor<A, T, B>.fromAv(av: AttributeValue) =
            builder.setter(converter.fromAv(av))

        item.forEach { (name, av) ->
            // TODO make behavior for unknown attributes configurable (ignore, exception, other?)
            descriptors[name]?.fromAv(av)
        }

        return builder.build()
    }

    override fun toItem(obj: T, onlyAttributes: Set<String>?): Item {
        /**
         * This is a convenience function to keep the compile-time safety for type param `A`. Without this, the compiler
         * can't track generic types across multiple statements:
         *
         * ```kotlin
         * val descriptor = descriptors[name] // AttributeDescriptor<*, T, B>
         * val value = descriptor.getter(obj) // Any?
         * descriptor.converter.toAv(value) // Type mismatch for value. Required: Nothing, Found: Any?
         * ```
         */
        fun <A> AttributeDescriptor<A, T, B>.toAv() =
            converter.toAv(getter(obj))

        val descriptors = if (onlyAttributes == null) {
            this.descriptors.values
        } else {
            this.descriptors.filterKeys(onlyAttributes::contains).values
        }

        return buildItem {
            descriptors.forEach { desc -> put(desc.name, desc.toAv()) }
        }
    }
}
