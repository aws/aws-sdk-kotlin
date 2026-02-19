/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.ModelAttributes
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.util.plus

/**
 * Derives a high-level [Operation] equivalent for this low-level operation
 * @param ctx The active [RenderContext]
 */
internal fun Operation.toHighLevel(ctx: RenderContext): Operation {
    val llOperation = this@toHighLevel
    val hlRequest = llOperation.request.toHighLevel(ctx)
    val hlResponse = llOperation.response.toHighLevel(ctx)
    val hlAttributes = llOperation.attributes + (ModelAttributes.LowLevelOperation to llOperation)
    return Operation(llOperation.methodName, hlRequest, hlResponse, hlAttributes)
}
