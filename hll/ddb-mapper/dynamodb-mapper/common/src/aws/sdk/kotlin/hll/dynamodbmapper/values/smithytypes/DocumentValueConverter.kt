/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.asNullable
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.AttributeValueListValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.AttributeValueMapValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.ListMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.MapMappingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.content.Document

/**
 * Converts between [Document] and various DynamoDB value types. The following conversions are performed:
 * * `null` ↔ [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null)
 * * [Document.Number] ↔ [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 * * [Document.String] ↔ [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 * * [Document.Boolean] ↔ [DynamoDB `BOOL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Boolean)
 * * [Document.List] ↔ [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List)
 * * [Document.Map] ↔ [DynamoDB `M` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.Map)
 */
public class DocumentValueConverter(
    private val numberValueConverter: ValueConverter<Number> = NumberValueConverters.Auto,
    private val stringValueConverter: ValueConverter<String> = StringValueConverter,
    private val booleanValueConverter: ValueConverter<Boolean> = BooleanValueConverter,
    nullWrapper: (ValueConverter<Document>) -> ValueConverter<Document?> = { it.asNullable() },
    attributeValueListValueConverter: ValueConverter<List<AttributeValue>> = AttributeValueListValueConverter,
    attributeValueMapValueConverter: ValueConverter<Map<String, AttributeValue>> = AttributeValueMapValueConverter,
) : ValueConverter<Document> {
    private val nullableConverter = nullWrapper(this)
    private val listValueConverter = ListMappingConverter(nullableConverter) + attributeValueListValueConverter
    private val mapValueConverter = MapMappingConverter(Converter.identity<String>(), nullableConverter) + attributeValueMapValueConverter

    public companion object {
        /**
         * The default instance of [DocumentValueConverter]
         */
        public val Default: DocumentValueConverter = DocumentValueConverter()
    }

    override val left: MonoConverter<AttributeValue, Document> = MonoConverter {
        when (it) {
            is AttributeValue.N -> Document.Number(numberValueConverter.convertLeft(it))
            is AttributeValue.S -> Document.String(stringValueConverter.convertLeft(it))
            is AttributeValue.Bool -> Document.Boolean(booleanValueConverter.convertLeft(it))
            is AttributeValue.L -> Document.List(listValueConverter.convertLeft(it))
            is AttributeValue.M -> Document.Map(mapValueConverter.convertLeft(it))
            else -> throw IllegalArgumentException("Documents do not support ${it::class.qualifiedName} values")
        }
    }

    override val right: MonoConverter<Document, AttributeValue> = MonoConverter {
        when (it) {
            is Document.Number -> numberValueConverter.convertRight(it.value)
            is Document.String -> stringValueConverter.convertRight(it.value)
            is Document.Boolean -> booleanValueConverter.convertRight(it.value)
            is Document.List -> listValueConverter.convertRight(it.value)
            is Document.Map -> mapValueConverter.convertRight(it.value)
        }
    }
}
