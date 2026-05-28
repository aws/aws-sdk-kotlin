/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.NullValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.AttributeValueListValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.AttributeValueMapValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes.DocumentConverterBase
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.content.Document

/**
 * Converts between [Document] and [Item]. The top-level document must be a [Document.Map]; nested values use the
 * same element-level conversion as [DocumentValueConverter][aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes.DocumentValueConverter].
 */
public class DocumentItemConverter(
    numberValueConverter: ValueConverter<Number> = NumberValueConverters.Auto,
    stringValueConverter: ValueConverter<String> = StringValueConverter,
    booleanValueConverter: ValueConverter<Boolean> = BooleanValueConverter,
    nullValueConverter: ValueConverter<Nothing?> = NullValueConverter,
    attributeValueListValueConverter: ValueConverter<List<AttributeValue>> = AttributeValueListValueConverter,
    attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
) : DocumentConverterBase(
    numberValueConverter,
    stringValueConverter,
    booleanValueConverter,
    nullValueConverter,
    attributeValueListValueConverter,
    attributeValueMapValueConverter,
), ItemConverter<Document> {
    public companion object {
        public val Default: DocumentItemConverter = DocumentItemConverter()
    }

    override fun convertLeft(from: Item): Document =
        Document.Map(from.mapValues { (_, attr) -> documentFromAttributeValue(attr) })

    override fun convertRight(from: Document): Item {
        require(from is Document.Map) { "DocumentItemConverter requires a Document.Map at the top level" }
        return from.mapValues { (_, value) -> attributeValueFromDocument(value) }.toItem()
    }
}
