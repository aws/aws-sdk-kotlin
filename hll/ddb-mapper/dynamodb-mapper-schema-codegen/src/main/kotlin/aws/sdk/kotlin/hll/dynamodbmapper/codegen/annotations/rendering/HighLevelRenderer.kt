/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param ctx The base render context, shared by all items
 * @param items The resolved IR for every annotated class to generate code for
 */
internal class HighLevelRenderer(
    private val ctx: RenderContext,
    private val items: List<MappedItem>,
) {
    internal fun render() {
        items.forEach { item ->
            SchemaRenderer(ctx.copy(pkg = item.destinationPkg), item).render()
        }
    }
}
