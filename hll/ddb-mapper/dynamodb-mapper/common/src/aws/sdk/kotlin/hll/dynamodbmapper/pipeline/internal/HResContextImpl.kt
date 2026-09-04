/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HResContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.util.requireNull

internal data class HResContextImpl<T, S : ItemSchema<T>, HReq, LReq, LRes, HRes>(
    override val highLevelRequest: HReq,
    override val serializeSchema: S,
    override val mapperContext: MapperContext<T>,
    override val lowLevelRequest: LReq,
    override val lowLevelResponse: LRes,
    override val deserializeSchema: S,
    override val highLevelResponse: HRes,
    override val error: Throwable?,
) : HResContext<T, S, HReq, LReq, LRes, HRes>,
    ErrorCombinable<HResContextImpl<T, S, HReq, LReq, LRes, HRes>>,
    Combinable<HResContextImpl<T, S, HReq, LReq, LRes, HRes>, HRes> {

    override fun plus(e: Throwable?) = copy(error = e.suppressing(error))
    override fun plus(value: HRes) = copy(highLevelResponse = value)
}

internal fun <T, S : ItemSchema<T>, HReq, LReq, LRes, HRes> HResContextImpl<T, S, HReq, LReq, LRes, HRes?>.solidify() = HResContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    lowLevelRequest,
    lowLevelResponse,
    deserializeSchema,
    requireNotNull(highLevelResponse) { "Cannot solidify context with a null high-level response" } as HRes,
    requireNull(error) { "Cannot solidify context with a non-null error" },
)
