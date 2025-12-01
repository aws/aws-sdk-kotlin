/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemResponse
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.HResContextImpl
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest as LowLevelGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemResponse as LowLevelGetItemResponse

/**
 * Contextual data for stages in the pipeline dealing with high-level responses (i.e., after deserialization)
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param LReq The type of low-level request object (e.g., [LowLevelGetItemRequest])
 * @param LRes The type of low-level response object (e.g., [LowLevelGetItemResponse])
 * @param HRes The type of high-level response object (e.g., [GetItemResponse])
 */
public interface HResContext<T, HReq, LReq, LRes, HRes> : LResContext<T, HReq, LReq, LRes> {
    /**
     * The high-level response to return to the caller
     */
    public val highLevelResponse: HRes
}

/**
 * Creates a new [HResContext]
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param LReq The type of low-level request object (e.g., [LowLevelGetItemRequest])
 * @param LRes The type of low-level response object (e.g., [LowLevelGetItemResponse])
 * @param highLevelRequest The high-level request object which is to be serialized into a low-level request object
 * @param serializeSchema The [ItemSchema] to use for serializing objects into items
 * @param mapperContext Additional, generalized context which may be useful to interceptors
 * @param lowLevelRequest The low-level request object which is to be used in the low-level operation invocation
 * @param lowLevelResponse The low-level response which is to be deserialized into a high-level response object
 * @param deserializeSchema The [ItemSchema] to use for deserializing items into objects
 * @param highLevelResponse The high-level response to return to the caller
 * @param error The most recent error which occurred, if any. Defaults to null.
 */
public fun <T, HReq, LReq, LRes, HRes> HResContext(
    highLevelRequest: HReq,
    serializeSchema: ItemSchema<T>,
    mapperContext: MapperContext<T>,
    lowLevelRequest: LReq,
    lowLevelResponse: LRes,
    deserializeSchema: ItemSchema<T>,
    highLevelResponse: HRes,
    error: Throwable? = null,
): HResContext<T, HReq, LReq, LRes, HRes> = HResContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    lowLevelRequest,
    lowLevelResponse,
    deserializeSchema,
    highLevelResponse,
    error,
)
