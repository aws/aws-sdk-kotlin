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
 * @param pkg The Kotlin package to use for the high-level operation's request and response structures
 */
internal fun Operation.toHighLevel(pkg: String, ctx: RenderContext): Operation {
    val llOperation = this@toHighLevel
    val hlRequest = llOperation.request.toHighLevel(pkg, ctx)
    val hlResponse = llOperation.response.toHighLevel(pkg, ctx)
    val hlAttributes = llOperation.attributes + (ModelAttributes.LowLevelOperation to llOperation)
    return Operation(llOperation.methodName, hlRequest, hlResponse, hlAttributes)
}
