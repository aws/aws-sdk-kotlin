/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.LReqContextImpl
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest as LowLevelGetItemRequest

/**
 * Contextual data for stages in the pipeline dealing with low-level requests (i.e., between serialization and low-level
 * invocation)
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param LReq The type of low-level request object (e.g., [LowLevelGetItemRequest])
 */
public interface LReqContext<T, HReq, LReq> : HReqContext<T, HReq> {
    /**
     * The low-level request object which is to be used in the low-level operation invocation
     */
    public val lowLevelRequest: LReq
}

/**
 * Creates a new [LReqContext]
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param LReq The type of low-level request object (e.g., [LowLevelGetItemRequest])
 * @param highLevelRequest The high-level request object which is to be serialized into a low-level request object
 * @param serializeSchema The [ItemSchema] to use for serializing objects into items
 * @param mapperContext Additional, generalized context which may be useful to interceptors
 * @param lowLevelRequest The low-level request object which is to be used in the low-level operation invocation
 * @param error The most recent error which occurred, if any. Defaults to null.
 */
public fun <T, HReq, LReq> LReqContext(
    highLevelRequest: HReq,
    serializeSchema: ItemSchema<T>,
    mapperContext: MapperContext<T>,
    lowLevelRequest: LReq,
    error: Throwable? = null,
): LReqContext<T, HReq, LReq> = LReqContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    lowLevelRequest,
    error,
)
