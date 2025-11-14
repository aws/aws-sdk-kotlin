/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.plus
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Converts between [Url] and [String] types
 */
@ExperimentalApi
public val UrlToStringConverter: Converter<Url, String> = Converter(Url::toString, Url::parse)

/**
 * Converts between [Url] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val UrlValueConverter: ValueConverter<Url> = UrlToStringConverter + StringValueConverter
