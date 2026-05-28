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
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.content.Document

/**
 * Converts between [Document]`?` and various DynamoDB attribute value types.
 */
public class DocumentValueConverter(
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
), ValueConverter<Document?> {
    public companion object {
        public val Default: DocumentValueConverter = DocumentValueConverter()
    }

    override fun convertLeft(from: AttributeValue): Document? = documentFromAttributeValue(from)
    override fun convertRight(from: Document?): AttributeValue = attributeValueFromDocument(from)
}
