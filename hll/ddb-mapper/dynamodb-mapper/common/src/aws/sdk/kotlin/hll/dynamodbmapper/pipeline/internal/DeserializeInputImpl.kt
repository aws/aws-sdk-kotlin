/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.DeserializeInput

internal data class DeserializeInputImpl<T, S : ItemSchema<T>, LRes>(
    override val lowLevelResponse: LRes,
    override val deserializeSchema: S,
) : DeserializeInput<T, S, LRes>
