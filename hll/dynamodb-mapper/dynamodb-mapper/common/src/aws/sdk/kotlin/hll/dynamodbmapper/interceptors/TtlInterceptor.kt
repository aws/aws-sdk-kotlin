/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.util.attr
import aws.smithy.kotlin.runtime.time.Clock
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest

/**
 * Interceptor that handles TTL fields defined on schema attributes and sets them to the current time plus their specified lifetimes.
 */
public class TtlInterceptor<T>(
    private val clock: Clock = Clock.System,
) : Interceptor<T, ItemSchema<T>, Any, Any, Any, Any> {

    override fun modifyBeforeInvocation(ctx: LReqContext<T, ItemSchema<T>, Any, Any>): Any {
        val ttlFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.TtlFields) ?: return ctx.lowLevelRequest

        return when (val request = ctx.lowLevelRequest) {
            is LowLevelPutItemRequest -> handlePutItem(request, ttlFields)
            // TODO Support UpdateItem
            else -> request
        }
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, ttlFields: Set<Pair<String, Long>>): LowLevelPutItemRequest {
        val newItem = request.item?.toMutableMap() ?: return request

        ttlFields.forEach { (fieldName, lifetime) ->
            val ttlValue = attr(clock.now().epochSeconds + lifetime)
            newItem[fieldName] = ttlValue
        }

        return request.copy { item = newItem }
    }
}
