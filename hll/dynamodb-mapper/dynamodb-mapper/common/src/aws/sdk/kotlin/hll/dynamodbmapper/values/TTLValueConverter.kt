/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * A value converter for time-to-live (TTL) fields which automatically sets the value
 * to current time + lifetime during conversion.
 *
 * @param lifetimeSeconds The time-to-live of the item, in seconds
 */
public class TTLValueConverter(private val lifetimeSeconds: Long) : ValueConverter<Long> {
    override val left: MonoConverter<AttributeValue, Long> = MonoConverter {
        it.asN().toLong()
    }

    override val right: MonoConverter<Long, AttributeValue> = MonoConverter {
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val ttlValue = currentTimeSeconds + lifetimeSeconds
        AttributeValue.N(ttlValue.toString())
    }
}
