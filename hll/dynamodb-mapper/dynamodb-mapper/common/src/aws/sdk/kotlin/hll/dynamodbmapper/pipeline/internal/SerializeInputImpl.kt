/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.SerializeInput

internal data class SerializeInputImpl<T, S : ItemSchema<T>, HReq>(
    override val highLevelRequest: HReq,
    override val serializeSchema: S,
) : SerializeInput<T, S, HReq>

internal operator fun <T, S : ItemSchema<T>, HReq> SerializeInput<T, S, HReq>.plus(mapperContext: MapperContext<T>) =
    HReqContextImpl(highLevelRequest, serializeSchema, mapperContext, null)
