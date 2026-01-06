/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.time.Clock

/**
 * A value converter for time-to-live (TTL) fields which automatically sets the value
 * to current time + lifetime during conversion.
 *
 * @param lifetimeSeconds The time-to-live of the item, in seconds
 * @param clock The clock to use for time calculations, defaults to [Clock.System]
 */
public class TTLValueConverter(
    private val lifetimeSeconds: Long,
    private val clock: Clock = Clock.System,
) : ValueConverter<Long> {
    init {
        require(lifetimeSeconds > 0) { "TTL must be positive, got $lifetimeSeconds seconds" }
    }
    
    override val left: MonoConverter<AttributeValue, Long> = MonoConverter {
        it.asN().toLong()
    }

    override val right: MonoConverter<Long, AttributeValue> = MonoConverter {
        val currentTimeSeconds = clock.now().epochSeconds
        val ttlValue = currentTimeSeconds + lifetimeSeconds
        AttributeValue.N(ttlValue.toString())
    }
}
