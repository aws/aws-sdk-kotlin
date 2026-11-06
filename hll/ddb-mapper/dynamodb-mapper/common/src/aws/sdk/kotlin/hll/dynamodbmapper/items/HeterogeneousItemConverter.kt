/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.reflect.KClass

/**
 * An item converter which handles heterogeneous (i.e., incongruent) data types by way of a string discriminator
 * attribute identified by [typeAttribute]. Each type name stored in [typeAttribute] identifies the delegate converter
 * (from [subConverters]) used for that item.
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
 *     val manufacturer: String,
 *     val gears: Int,
 *     val isElectric: Boolean,
 * ) : Vehicle
 * ```
 *
 * A heterogeneous item converter can be constructed by registering a converter for each type:
 *
 * ```kotlin
 * val vehicleConverter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "type") {
 *     instanceOf("car", CarConverter)
 *     instanceOf("bike", BikeConverter)
 * }
 * ```
 *
 * See [HeterogeneousItemConverter.Builder] for details on how objects are matched to registered types.
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
 *     table.putItem(vehicle)
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
 * Converters may alternatively be constructed with an explicit [typeMapper] function via the primary constructor. This
 * is useful when the type name isn't determined by the object's class (e.g., when it's derived from a property value).
 *
 * All conversion failures (unmapped objects, missing/non-string/unknown type attributes, and discriminator conflicts)
 * throw [IllegalArgumentException].
 *
 * @param T The common type ancestor for all subtypes handled by this converter. This may be a base class, interface, or
 * even [Any].
 * @property typeMapper A function which accepts an instance of the common type [T] and returns the string identifier
 * for the type. This identifier is written/read from the attribute identified by [typeAttribute] and used as a lookup
 * key in [subConverters].
 * @property typeAttribute The name of the attribute in which to store/read type information. This attribute will be
 * present for every item persisted via this converter.
 * @property subConverters A map of type names to the [ItemConverter] instances which handle them. Each converter may
 * handle [T] itself or any subtype of [T] (e.g., generated converters for individual subclasses).
 */
public class HeterogeneousItemConverter<T> internal constructor(
    public val typeMapper: (T) -> String,
    public val typeAttribute: String,
    public val subConverters: Map<String, ItemConverter<out T>>,
    // When true (builder-created converters), typeAttribute belongs solely to this converter: subconverters may not
    // write it and never see it on read. When false (the public constructor), typeAttribute is passed through to
    // subconverters on read and they may write it as long as the value matches, preserving pre-builder behavior.
    private val exclusiveTypeAttribute: Boolean,
) : ItemConverter<T> {
    /**
     * Initializes a new [HeterogeneousItemConverter] with an explicit type-mapping function
     * @param typeMapper A function which accepts an instance of the common type [T] and returns the string identifier
     * for the type. This identifier is written/read from the attribute identified by [typeAttribute] and used as a
     * lookup key in [subConverters].
     * @param typeAttribute The name of the attribute in which to store/read type information. Items passed to
     * subconverters during reads include this attribute. Subconverters may also write this attribute but only if the
     * value written matches the type name returned by [typeMapper].
     * @param subConverters A map of type names (the same returned by [typeMapper]) to [ItemConverter] instances. The
     * converter registered under a type name must accept every object for which [typeMapper] returns that name;
     * otherwise a [ClassCastException] may be thrown during conversion.
     */
    public constructor(
        typeMapper: (T) -> String,
        typeAttribute: String,
        subConverters: Map<String, ItemConverter<out T>>,
    ) : this(typeMapper, typeAttribute, subConverters, exclusiveTypeAttribute = false)

    init {
        require(typeAttribute.isNotBlank()) { "typeAttribute must not be blank" }
        require(subConverters.isNotEmpty()) { "At least one subconverter must be specified" }
    }

    override fun convertLeft(from: Item): T {
        val attr = requireNotNull(from[typeAttribute]) { """Item is missing type attribute "$typeAttribute"""" }
        val typeName = requireNotNull(attr.asSOrNull()) { """Type attribute "$typeAttribute" is not a string: $attr""" }
        val converter = converterFor(typeName)

        val subItem = if (exclusiveTypeAttribute) (from - typeAttribute).toItem() else from
        return converter.convertLeft(subItem)
    }

    override fun convertRight(from: T): Item {
        val typeName = typeMapper(from)

        // Converters may be registered for subtypes of T. For builder-created converters, typeMapper only returns the
        // name of a registration whose class `from` is an instance of. For the public constructor, that's the caller's contract.
        @Suppress("UNCHECKED_CAST")
        val converter = converterFor(typeName) as ItemConverter<T>

        val subItem = converter.convertRight(from)
        val typeValue = AttributeValue.S(typeName)
        subItem[typeAttribute]?.let { emitted ->
            require(!exclusiveTypeAttribute) {
                """Subconverter for type "$typeName" wrote reserved type attribute "$typeAttribute""""
            }
            require(emitted == typeValue) {
                """Subconverter for type "$typeName" wrote conflicting value $emitted to type attribute "$typeAttribute""""
            }
        }

        return buildItem {
            putAll(subItem)
            put(typeAttribute, typeValue)
        }
    }

    private fun converterFor(typeName: String) = requireNotNull(subConverters[typeName]) {
        """No converter for type "$typeName""""
    }

    /**
     * A builder for a [HeterogeneousItemConverter] which maps objects to type names according to their class. Obtain
     * an instance via the `HeterogeneousItemConverter(typeAttribute) { ... }` factory function.
     *
     * Registrations are evaluated in the order they were made and the first one which matches an object wins,
     * regardless of whether it was registered via [exactType] or [instanceOf]. Consequently, register more specific
     * types before less specific ones (e.g., `exactType<Car>` before `instanceOf<Car>`, and `instanceOf<Car>` before
     * `instanceOf<Vehicle>`). Registrations which are made unreachable by an earlier registration of the _same_ class
     * are rejected. Registrations made unreachable by an earlier registration of a _supertype_ cannot be detected.
     * @param T The common type ancestor for all types handled by the resulting converter
     */
    public class Builder<T : Any> internal constructor() {
        private class Registration(val typeName: String, val kClass: KClass<*>, val exact: Boolean) {
            fun matches(obj: Any): Boolean = if (exact) obj::class == kClass else kClass.isInstance(obj)
        }

        private val registrations = mutableListOf<Registration>()
        private val subConverters = mutableMapOf<String, ItemConverter<out T>>()

        /**
         * Registers [converter] for objects whose class is exactly [S] (i.e., _not_ instances of subtypes of [S]).
         *
         * Note that no object's class is exactly an interface or abstract class, so registering such a type via this
         * method will never match. Similarly, runtime-generated subclasses (e.g., proxies or mocks) will not match.
         * @param S The type to register
         * @param typeName The value stored in the type attribute for matching objects
         * @param converter The [ItemConverter] for matching objects
         */
        public inline fun <reified S : T> exactType(typeName: String, converter: ItemConverter<S>) {
            exactType(typeName, S::class, converter)
        }

        /**
         * Registers [converter] for objects whose class is exactly [kClass] (i.e., _not_ instances of subtypes of
         * [kClass]).
         *
         * Note that no object's class is exactly an interface or abstract class, so registering such a type via this
         * method will never match. Similarly, runtime-generated subclasses (e.g., proxies or mocks) will not match.
         * @param S The type to register
         * @param typeName The value stored in the type attribute for matching objects
         * @param kClass The class of [S]
         * @param converter The [ItemConverter] for matching objects
         */
        public fun <S : T> exactType(typeName: String, kClass: KClass<S>, converter: ItemConverter<S>) {
            register(Registration(typeName, kClass, exact = true), converter)
        }

        /**
         * Registers [converter] for objects which are instances of [S] (i.e., objects whose class is [S] or any
         * subtype of [S])
         * @param S The type to register
         * @param typeName The value stored in the type attribute for matching objects
         * @param converter The [ItemConverter] for matching objects
         */
        public inline fun <reified S : T> instanceOf(typeName: String, converter: ItemConverter<S>) {
            instanceOf(typeName, S::class, converter)
        }

        /**
         * Registers [converter] for objects which are instances of [kClass] (i.e., objects whose class is [kClass] or
         * any subtype of [kClass])
         * @param S The type to register
         * @param typeName The value stored in the type attribute for matching objects
         * @param kClass The class of [S]
         * @param converter The [ItemConverter] for matching objects
         */
        public fun <S : T> instanceOf(typeName: String, kClass: KClass<S>, converter: ItemConverter<S>) {
            register(Registration(typeName, kClass, exact = false), converter)
        }

        private fun register(registration: Registration, converter: ItemConverter<out T>) {
            require(registration.typeName !in subConverters) {
                """Type name "${registration.typeName}" is already registered"""
            }

            // An earlier instanceOf<K> matches everything a later registration of K would. An earlier exactType<K>
            // matches everything a later exactType<K> would but leaves subtypes of K for a later instanceOf<K>.
            val shadowing = registrations.firstOrNull {
                it.kClass == registration.kClass && (!it.exact || registration.exact)
            }
            require(shadowing == null) {
                """Registration of ${registration.kClass} as "${registration.typeName}" is unreachable because """ +
                    """it is shadowed by earlier registration "${shadowing!!.typeName}""""
            }

            registrations += registration
            subConverters[registration.typeName] = converter
        }

        internal fun build(typeAttribute: String): HeterogeneousItemConverter<T> {
            val registrations = registrations.toList()
            val typeMapper = { obj: T ->
                requireNotNull(registrations.firstOrNull { it.matches(obj) }) {
                    "No registered type matches ${obj::class}"
                }.typeName
            }

            return HeterogeneousItemConverter(typeMapper, typeAttribute, subConverters.toMap(), exclusiveTypeAttribute = true)
        }
    }
}

/**
 * Initializes a new [HeterogeneousItemConverter] which maps objects to type names according to their class
 * @param T The common type ancestor for all subtypes handled by this converter
 * @param typeAttribute The name of the attribute in which to store/read type information. This attribute is reserved
 * for use by the converter: subconverters must not write it and it is removed from items before being passed to
 * subconverters during reads.
 * @param block A DSL block which registers types via [HeterogeneousItemConverter.Builder.exactType] and
 * [HeterogeneousItemConverter.Builder.instanceOf]
 */
@Suppress("ktlint:standard:function-naming")
public fun <T : Any> HeterogeneousItemConverter(
    typeAttribute: String,
    block: HeterogeneousItemConverter.Builder<T>.() -> Unit,
): HeterogeneousItemConverter<T> = HeterogeneousItemConverter.Builder<T>().apply(block).build(typeAttribute)
