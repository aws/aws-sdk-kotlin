/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.NullValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.AttributeValueListValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.AttributeValueMapValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.collections.ListMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.MapMappingConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.content.Document

/**
 * Base class providing two-way conversion between [Document]`?` values and [AttributeValue]. Subclasses use
 * [documentFromAttributeValue] and [attributeValueFromDocument] to build higher-level converters (e.g., single-value
 * or item-level).
 *
 * @param numberValueConverter Converter for [Number] ↔ `N` attribute values
 * @param stringValueConverter Converter for [String] ↔ `S` attribute values
 * @param booleanValueConverter Converter for [Boolean] ↔ `BOOL` attribute values
 * @param nullValueConverter Converter for `null` ↔ `NULL` attribute values
 * @param attributeValueListValueConverter Converter for `List<AttributeValue>` ↔ `L` attribute values
 * @param attributeValueMapValueConverter Converter for `Map<String, AttributeValue>` ↔ `M` attribute values
 */
public abstract class DocumentConverterBase(
    private val numberValueConverter: ValueConverter<Number> = NumberValueConverters.Auto,
    private val stringValueConverter: ValueConverter<String> = StringValueConverter,
    private val booleanValueConverter: ValueConverter<Boolean> = BooleanValueConverter,
    private val nullValueConverter: ValueConverter<Nothing?> = NullValueConverter,
    attributeValueListValueConverter: ValueConverter<List<AttributeValue>> = AttributeValueListValueConverter,
    attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
) {
    /**
     * An element-level converter for [Document]`?` ↔ [AttributeValue]. This converter recursively handles nested
     * documents using the configured sub-converters.
     */
    protected val elementConverter: Converter<Document?, AttributeValue> = ElementConverter(
        numberValueConverter,
        stringValueConverter,
        booleanValueConverter,
        nullValueConverter,
        attributeValueListValueConverter,
        attributeValueMapValueConverter,
    )

    /** Converts an [AttributeValue] to a [Document]`?` */
    protected fun documentFromAttributeValue(from: AttributeValue): Document? = elementConverter.convertLeft(from)

    /** Converts a [Document]`?` to an [AttributeValue] */
    protected fun attributeValueFromDocument(from: Document?): AttributeValue = elementConverter.convertRight(from)

    private inner class ElementConverter(
        private val numberValueConverter: ValueConverter<Number>,
        private val stringValueConverter: ValueConverter<String>,
        private val booleanValueConverter: ValueConverter<Boolean>,
        private val nullValueConverter: ValueConverter<Nothing?>,
        attributeValueListValueConverter: ValueConverter<List<AttributeValue>>,
        attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>>,
    ) : Converter<Document?, AttributeValue> {
        private val listValueConverter = ConverterChain(ListMappingConverter(this), attributeValueListValueConverter)
        private val mapValueConverter = ConverterChain(MapMappingConverter(Converter.identity<String>(), this), attributeValueMapValueConverter)

        override fun convertLeft(from: AttributeValue): Document? = when (from) {
            is AttributeValue.Null -> nullValueConverter.convertLeft(from)
            is AttributeValue.N -> Document.Number(numberValueConverter.convertLeft(from))
            is AttributeValue.S -> Document.String(stringValueConverter.convertLeft(from))
            is AttributeValue.Bool -> Document.Boolean(booleanValueConverter.convertLeft(from))
            is AttributeValue.L -> Document.List(listValueConverter.convertLeft(from))
            is AttributeValue.M -> Document.Map(mapValueConverter.convertLeft(from))
            else -> throw IllegalArgumentException("Documents do not support ${from::class.qualifiedName} values")
        }

        override fun convertRight(from: Document?): AttributeValue = when (from) {
            null -> nullValueConverter.convertRight(from)
            is Document.Number -> numberValueConverter.convertRight(from.value)
            is Document.String -> stringValueConverter.convertRight(from.value)
            is Document.Boolean -> booleanValueConverter.convertRight(from.value)
            is Document.List -> listValueConverter.convertRight(from.value)
            is Document.Map -> mapValueConverter.convertRight(from.value)
        }
    }
}
