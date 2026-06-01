/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.util.requireNull

internal data class LReqContextImpl<T, S : ItemSchema<T>, HReq, LReq>(
    override val highLevelRequest: HReq,
    override val serializeSchema: S,
    override val mapperContext: MapperContext<T>,
    override val lowLevelRequest: LReq,
    override val error: Throwable?,
) : LReqContext<T, S, HReq, LReq>,
    ErrorCombinable<LReqContextImpl<T, S, HReq, LReq>>,
    Combinable<LReqContextImpl<T, S, HReq, LReq>, LReq> {

    override fun plus(e: Throwable?) = copy(error = e.suppressing(error))
    override fun plus(value: LReq) = copy(lowLevelRequest = value)
}

internal operator fun <T, S : ItemSchema<T>, HReq, LReq, LRes> LReqContext<T, S, HReq, LReq>.plus(
    lowLevelResponse: LRes,
) = LResContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    lowLevelRequest,
    lowLevelResponse,
    serializeSchema, // Use the already-resolved schema at first
    error,
)

internal fun <T, S : ItemSchema<T>, HReq, LReq> LReqContext<T, S, HReq, LReq?>.solidify() = LReqContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    requireNotNull(lowLevelRequest) { "Cannot solidify context with a null low-level request" } as LReq,
    requireNull(error) { "Cannot solidify context with a non-null error" },
)
