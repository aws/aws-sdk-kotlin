/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterChain
import aws.sdk.kotlin.hll.mapping.core.converters.ConverterImpl
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds

/**
 * Provides access to [ValueConverter] types for various [Instant] representations
 */
public object InstantValueConverter : ValueConverter<Instant> by EpochS {
    /**
     * Converts between [Instant] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * containing the number of milliseconds since the Unix epoch
     */
    public class EpochMs : ValueConverter<Instant> by EpochMs {
        public companion object : ConverterChain<Instant, Long, AttributeValue>(
            ConverterImpl(Instant::epochMilliseconds, Instant::fromEpochMilliseconds),
            NumberValueConverters.Long,
        )
    }

    /**
     * Converts between [Instant] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * containing the number of seconds since the Unix epoch
     */
    public class EpochS : ValueConverter<Instant> by EpochS {
        public companion object : ConverterChain<Instant, Long, AttributeValue>(
            ConverterImpl(Instant::epochSeconds, Instant::fromEpochSeconds),
            NumberValueConverters.Long,
        )
    }

    /**
     * Converts between [Instant] and
     * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
     * containing a formatted ISO 8601 representation
     */
    public class Iso8601 : ValueConverter<Instant> by Iso8601 {
        public companion object : ConverterChain<Instant, String, AttributeValue>(
            ConverterImpl({ it.format(TimestampFormat.ISO_8601_FULL) }, Instant::fromIso8601),
            StringValueConverter,
        )
    }
}
