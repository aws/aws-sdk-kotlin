/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.HReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.SerializeInput

internal data class HReqContextImpl<T, HReq>(
    override val highLevelRequest: HReq,
    override val serializeSchema: ItemSchema<T>,
    override val mapperContext: MapperContext<T>,
    override val error: Throwable? = null,
) : HReqContext<T, HReq>,
    ErrorCombinable<HReqContextImpl<T, HReq>>,
    Combinable<HReqContextImpl<T, HReq>, SerializeInput<T, HReq>> {

    override fun plus(e: Throwable?) = copy(error = e.suppressing(error))

    override fun plus(value: SerializeInput<T, HReq>) = copy(
        highLevelRequest = value.highLevelRequest,
        serializeSchema = value.serializeSchema,
    )
}

internal operator fun <T, HReq, LReq> HReqContext<T, HReq>.plus(lowLevelRequest: LReq) = LReqContextImpl(highLevelRequest, serializeSchema, mapperContext, lowLevelRequest, error)
