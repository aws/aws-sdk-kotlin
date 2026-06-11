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
import aws.sdk.kotlin.hll.dynamodbmapper.util.av
import aws.smithy.kotlin.runtime.time.Clock
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest as LowLevelPutItemRequest

/**
 * Interceptor that handles TTL fields defined on schema attributes and sets them to the current time plus their
 * specified lifetimes. This affects two operations:
 * * `PutItem` is intercepted in [modifyBeforeInvocation] to update the low-level request with the field names and their
 *   calculated values
 * * `UpdateItem` is intercepted in [modifyBeforeSerialization] to update the high-level request using the update
 *   expression
 */
public class TtlInterceptor(private val clock: Clock = Clock.System) : InterceptorAny {
    override fun modifyBeforeInvocation(ctx: LReqContext<Any, ItemSchema<Any>, Any, Any>): Any {
        val ttlFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.TtlFields) ?: return ctx.lowLevelRequest

        return when (val originalReq = ctx.lowLevelRequest) {
            is LowLevelPutItemRequest -> handlePutItem(originalReq, ttlFields)
            else -> originalReq
        }
    }

    override fun modifyBeforeSerialization(ctx: HReqContext<Any, ItemSchema<Any>, Any>): SerializeInput<Any, ItemSchema<Any>, Any> {
        val ttlFields = ctx.serializeSchema.attributes.getOrNull(SchemaAttributes.TtlFields) ?: return ctx

        val updatedReq = when (val originalReq = ctx.highLevelRequest) {
            is UpdateItemRequest -> handleUpdateItem(originalReq, ttlFields)
            else -> return ctx
        }

        return SerializeInput(updatedReq, ctx.serializeSchema)
    }

    private fun handlePutItem(request: LowLevelPutItemRequest, ttlFields: Map<String, Long>): LowLevelPutItemRequest {
        val item = request.item?.toMutableMap() ?: return request

        ttlFields.forEach { (fieldName, lifetime) ->
            val ttlValue = av(clock.now().epochSeconds + lifetime)
            item[fieldName] = ttlValue
        }

        return request.copy { this.item = item }
    }

    private fun handleUpdateItem(
        request: UpdateItemRequest,
        ttlFields: Map<String, Long>,
    ): UpdateItemRequest = when (request) {
        is UpdateItemRequest.PartitionKey<*> -> request.toBuilder().apply {
            update {
                set {
                    ttlFields.forEach { (fieldName, lifetime) ->
                        attr[fieldName] = clock.now().epochSeconds + lifetime
                    }
                }
            }
        }.build()

        is UpdateItemRequest.CompositeKey<*, *> -> request.toBuilder().apply {
            update {
                set {
                    ttlFields.forEach { (fieldName, lifetime) ->
                        attr[fieldName] = clock.now().epochSeconds + lifetime
                    }
                }
            }
        }.build()
    }
}
