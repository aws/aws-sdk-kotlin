/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.ec2
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.ClientOptionalTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * EC2 incorrectly models primitive shapes as unboxed when they actually
 * need to be boxed for the API to work properly (e.g. sending default values).
 * This integration pre-processes the model to make all members optional.
 *
 * See: https://github.com/awslabs/aws-sdk-kotlin/issues/261
 */
class EC2MakePrimitivesOptional : KotlinIntegration {
    override val order: Byte = -127
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        settings.service == ShapeId.from("com.amazonaws.ec2#AmazonEC2")

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val updates = mutableListOf<Shape>()
        for (struct in model.structureShapes) {
            for (member in struct.allMembers.values) {
                updates.add(member.toBuilder().addTrait(ClientOptionalTrait()).build())
            }
        }
        return ModelTransformer.create().replaceShapes(model, updates)
    }
}
