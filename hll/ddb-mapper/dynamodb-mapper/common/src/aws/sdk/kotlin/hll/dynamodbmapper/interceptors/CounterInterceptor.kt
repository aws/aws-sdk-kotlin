/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.interceptors

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.SchemaAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.operations.UpdateItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.toBuilder
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.InterceptorAny
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.SerializeInput
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest

/**
 * Interceptor that handles counter fields defined on schema attributes by incrementing them on each mutating operation.
 * This affects two operations:
 * * `PutItem` is intercepted in [modifyBeforeInvocation] to update the low-level request with the field names and their
 *   calculated values
 * * `UpdateItem` is intercepted in [modifyBeforeSerialization] to update the high-level request using the update
 *   expression
 */
public class CounterInterceptor : InterceptorAny {
    override fun modifyBeforeInvocation(ctx: LReqContext<Any, ItemSchema<Any>, Any, Any>): Any {
        val counterFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.CounterFields) ?: return ctx.lowLevelRequest

        return when (val originalReq = ctx.lowLevelRequest) {
            is LowLevelPutItemRequest -> handlePutItem(originalReq, counterFields)
            else -> originalReq
        }
    }

    override fun modifyBeforeSerialization(ctx: HReqContext<Any, ItemSchema<Any>, Any>): SerializeInput<Any, ItemSchema<Any>, Any> {
        val counterFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.CounterFields) ?: return ctx

        val updatedReq = when (val originalReq = ctx.highLevelRequest) {
            is UpdateItemRequest -> handleUpdateItem(originalReq, counterFields)
            else -> return ctx
        }

        return SerializeInput(updatedReq, ctx.serializeSchema)
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, counterFields: Set<String>): LowLevelPutItemRequest {
        val item = request.item?.toMutableMap() ?: return request

        counterFields.forEach { fieldName ->
            val currentValue = item[fieldName]?.asN()?.toLong() ?: 0L
            item[fieldName] = AttributeValue.N((currentValue + 1).toString())
        }

        return request.copy { this.item = item }
    }

    private fun handleUpdateItem(
        request: UpdateItemRequest,
        counterFields: Set<String>,
    ): UpdateItemRequest = when (request) {
        is UpdateItemRequest.PartitionKey<*> -> request.toBuilder().apply {
            update {
                set {
                    counterFields.forEach { fieldName ->
                        attr[fieldName] = attr[fieldName].orElse(0) + 1
                    }
                }
            }
        }.build()

        is UpdateItemRequest.CompositeKey<*, *> -> request.toBuilder().apply {
            update {
                set {
                    counterFields.forEach { fieldName ->
                        attr[fieldName] = attr[fieldName].orElse(0) + 1
                    }
                }
            }
        }.build()
    }
}
