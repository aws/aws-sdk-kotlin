/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Converts between [Url] and [String] types
 */
public class UrlToStringConverter : Converter<Url, String> by UrlToStringConverter {
    public companion object : Converter<Url, String> {
        override fun convertLeft(from: String): Url = Url.parse(from)
        override fun convertRight(from: Url): String = from.toString()
    }
}

/**
 * Converts between [Url] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class UrlValueConverter : ValueConverter<Url> by UrlValueConverter {
    public companion object : ConverterChain<Url, String, AttributeValue>(UrlToStringConverter, StringValueConverter)
}
