/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext

internal data class MapperContextImpl<T>(
    override val persistenceSpec: PersistenceSpec<T>,
    override val operation: String,
) : MapperContext<T>
