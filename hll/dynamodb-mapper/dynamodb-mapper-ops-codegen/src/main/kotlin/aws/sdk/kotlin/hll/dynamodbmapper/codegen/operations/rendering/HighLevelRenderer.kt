/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.ItemSourceKind
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.dataType
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.itemSourceKinds

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param ctx The active [RenderContext]
 * @param operations A list of the operations in scope for codegen
 */
internal class HighLevelRenderer(private val ctx: RenderContext, private val operations: List<Operation>) {
    fun render() {
        operations.forEach(::render)

        val kindTypes = mutableMapOf<ItemSourceKind, Type>()
        ItemSourceKind.entries.forEach { kind ->
            val parentType = kind.parent?.let { kindTypes[it] }
            val operations = this.operations.filter { kind in it.itemSourceKinds }

            val renderer = OperationsTypeRenderer(ctx, kind, parentType, operations)
            renderer.render()
            kindTypes += kind to renderer.unkeyedType
        }
    }

    private fun render(operation: Operation) {
        RequestRenderer(ctx, operation.request.dataType).render()
        ResponseRenderer(ctx, operation.response.dataType).render()
        FactoryRenderer(ctx, operation).render()
    }
}
