/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.SerializeInput

internal data class SerializeInputImpl<T, HReq>(
    override val highLevelRequest: HReq,
    override val serializeSchema: ItemSchema<T>,
) : SerializeInput<T, HReq>

internal operator fun <T, HReq> SerializeInput<T, HReq>.plus(mapperContext: MapperContext<T>) = HReqContextImpl(highLevelRequest, serializeSchema, mapperContext, null)
