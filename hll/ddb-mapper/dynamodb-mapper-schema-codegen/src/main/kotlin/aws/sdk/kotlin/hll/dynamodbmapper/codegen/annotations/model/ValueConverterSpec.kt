/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model

import aws.sdk.kotlin.hll.codegen.model.TypeRef

/**
 * A fully-resolved description of the value converter to instantiate for an attribute. Rendering a spec requires no
 * further decisions.
 */
internal sealed interface ValueConverterSpec {
    /**
     * A converter object referenced as-is (e.g., `IntValueConverter`)
     */
    data class Scalar(val converter: TypeRef) : ValueConverterSpec

    /**
     * Wraps [inner] to support a nullable value (`NullableValueConverter(inner)`)
     */
    data class Nullable(val inner: ValueConverterSpec) : ValueConverterSpec

    /**
     * An enum value converter for [type] (`enumValueConverter<E>()`)
     */
    data class Enum(val type: TypeRef) : ValueConverterSpec

    /**
     * A list converter for elements converted by [element] (`ListValueConverter(element)`)
     */
    data class ListConverter(val element: ValueConverterSpec) : ValueConverterSpec

    /**
     * A map converter (`MapValueConverter(key, value)`)
     */
    data class MapConverter(val key: MapKeyConverterSpec, val value: ValueConverterSpec) : ValueConverterSpec

    /**
     * A set converter object referenced as-is (e.g., `StringSetValueConverter`)
     */
    data class SetConverter(val converter: TypeRef) : ValueConverterSpec

    /**
     * A set converter for enum elements of [type] (`EnumSetValueConverter<E>()`)
     */
    data class EnumSetConverter(val type: TypeRef) : ValueConverterSpec

    /**
     * The generated value converter of a nested mapped type
     */
    data class NestedRef(val valueConverter: TypeRef) : ValueConverterSpec

    /**
     * A user-provided converter from `@DynamoDbAttributeConverter`. Classes are instantiated; objects are referenced
     * as-is.
     */
    data class Custom(val converter: TypeRef, val needsCtorInvoke: Boolean) : ValueConverterSpec
}

/**
 * A fully-resolved description of the converter used for map keys. DynamoDB map keys are strings, so key converters are
 * a distinct, smaller family than [ValueConverterSpec].
 */
internal sealed interface MapKeyConverterSpec {
    /**
     * A key converter object referenced as-is (e.g., `IntToStringConverter`)
     */
    data class Scalar(val converter: TypeRef) : MapKeyConverterSpec

    /**
     * An enum key converter for [type] (`EnumToStringConverter<E>()`)
     */
    data class Enum(val type: TypeRef) : MapKeyConverterSpec
}
