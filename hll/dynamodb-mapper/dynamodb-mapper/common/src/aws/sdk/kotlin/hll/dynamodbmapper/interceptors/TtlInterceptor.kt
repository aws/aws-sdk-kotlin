/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.time.Clock
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest

/**
 * Interceptor that handles a TTL field defined on schema attributes and sets it to the current time plus the specified lifetime.
 */
public class TtlInterceptor<T>(
    private val clock: Clock = Clock.System,
) : Interceptor<T, Any, Any, Any, Any> {

    override fun modifyBeforeInvocation(ctx: LReqContext<T, Any, Any>): Any {
        val ttlField = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.TtlField) ?: return ctx.lowLevelRequest

        return when (val request = ctx.lowLevelRequest) {
            is LowLevelPutItemRequest -> handlePutItem(request, ttlField)
            // TODO Support UpdateItem
            else -> request
        }
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, ttlField: Pair<String, Long>): LowLevelPutItemRequest {
        val newItem = request.item?.toMutableMap() ?: return request
        val (fieldName, lifetime) = ttlField

        val ttlValue = AttributeValue.N((clock.now().epochSeconds + lifetime).toString())
        newItem[fieldName] = ttlValue

        return request.copy { item = newItem }
    }
}
