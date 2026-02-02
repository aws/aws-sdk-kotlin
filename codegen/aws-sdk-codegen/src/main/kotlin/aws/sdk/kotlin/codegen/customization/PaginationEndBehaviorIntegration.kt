/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.traits.PaginationEndBehavior
import aws.smithy.kotlin.codegen.model.traits.PaginationEndBehaviorTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.BooleanShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.transform.ModelTransformer

private val TRUNCATION_MEMBER_IDS = mapOf(
    "com.amazonaws.s3#ListParts" to "IsTruncated",
)

private val IDENTICAL_TOKEN_OPERATION_IDS = setOf(
    "com.amazonaws.cloudwatchlogs#GetLogEvents", // https://github.com/awslabs/aws-sdk-kotlin/issues/1326
)

/**
 * Applies [PaginationEndBehaviorTrait] to a manually-curated list of operations/members for which pagination terminates
 * in a non-standard manner
 */
class PaginationEndBehaviorIntegration : KotlinIntegration {
    override fun preprocessModel(model: Model, settings: KotlinSettings): Model = ModelTransformer
        .create()
        .mapShapes(model) { shape ->
            val shapeId = shape.id.toString()
            when {
                shape !is OperationShape -> shape // Pagination behavior trait only applied to operations

                shapeId in TRUNCATION_MEMBER_IDS -> {
                    val output = model.expectShape(shape.outputShape)
                    require(output is StructureShape) { "Operation output must be a structure shape" }
                    val memberName = TRUNCATION_MEMBER_IDS.getValue(shapeId)
                    val member = output.allMembers[memberName] ?: error("Cannot find $memberName in ${output.id}")
                    val target = model.expectShape(member.target)
                    check(target is BooleanShape) { "Truncation member must be a boolean shape" }

                    val behavior = PaginationEndBehavior.TruncationMember(memberName)
                    shape.toBuilder().addTrait(PaginationEndBehaviorTrait(behavior)).build()
                }

                shapeId in IDENTICAL_TOKEN_OPERATION_IDS ->
                    shape.toBuilder().addTrait(PaginationEndBehaviorTrait(PaginationEndBehavior.IdenticalToken)).build()

                else -> shape
            }
        }
}
