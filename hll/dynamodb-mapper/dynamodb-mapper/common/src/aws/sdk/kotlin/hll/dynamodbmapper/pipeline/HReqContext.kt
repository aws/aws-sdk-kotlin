/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.HReqContextImpl

/**
 * Contextual data for stages in the pipeline dealing with high-level requests (i.e., before serialization)
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 */
public interface HReqContext<T, HReq> : SerializeInput<T, HReq> {

    /**
     * Additional, generalized context which may be useful to interceptors
     */
    public val mapperContext: MapperContext<T>

    /**
     * The most recent error which occurred, if any. If another error occurs while this property is already set, it will
     * be replaced with the new error and the old error will be added as a suppression
     * (i.e., [Throwable.addSuppressed]).
     */
    public val error: Throwable?
}

/**
 * Creates a new [HReqContext]
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param highLevelRequest The high-level request object which is to be serialized into a low-level request object
 * @param serializeSchema The [ItemSchema] to use for serializing objects into items
 * @param mapperContext Additional, generalized context which may be useful to interceptors
 * @param error The most recent error which occurred, if any. Defaults to null.
 */
public fun <T, HReq> HReqContext(
    highLevelRequest: HReq,
    serializeSchema: ItemSchema<T>,
    mapperContext: MapperContext<T>,
    error: Throwable? = null,
): HReqContext<T, HReq> = HReqContextImpl(highLevelRequest, serializeSchema, mapperContext, error)
