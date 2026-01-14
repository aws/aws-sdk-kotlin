/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest

/**
 * Interceptor that handles counter fields defined on schema attributes by incrementing them on each mutating operation
 */
public class CounterInterceptor<T> : Interceptor<T, Any, Any, Any, Any> {

    override fun modifyBeforeInvocation(ctx: LReqContext<T, Any, Any>): Any {
        val counterFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.CounterFields) ?: return ctx.lowLevelRequest

        return when (val request = ctx.lowLevelRequest) {
            is LowLevelPutItemRequest -> handlePutItem(request, counterFields)
            // TODO Support UpdateItem
            else -> request
        }
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, counterFields: Set<String>): LowLevelPutItemRequest {
        val newItem = request.item?.toMutableMap() ?: return request

        counterFields.forEach { fieldName ->
            val currentValue = newItem[fieldName]?.asN()?.toLong() ?: 0L
            newItem[fieldName] = AttributeValue.N((currentValue + 1).toString())
        }

        return request.copy { item = newItem }
    }
}
