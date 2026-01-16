/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.hasTrait
import aws.smithy.kotlin.codegen.model.traits.UnwrappedXmlOutput
import software.amazon.smithy.aws.traits.customizations.S3UnwrappedXmlOutputTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Applies the custom [UnwrappedXmlOutput]
 * [annotation trait](https://smithy.io/2.0/spec/model.html?highlight=annotation#annotation-traits) to operations
 * annotated with `S3UnwrappedXmlOutput` trait to mark when special unwrapped xml output deserialization is required.
 */
class UnwrappedXmlOutputIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val unwrappedOperations = model
            .operationShapes
            .filter { it.hasTrait<S3UnwrappedXmlOutputTrait>() }
            .map { it.id }
            .toSet()

        return ModelTransformer
            .create()
            .mapShapes(model) { shape ->
                when {
                    shape.id in unwrappedOperations ->
                        (shape as OperationShape).toBuilder().addTrait(UnwrappedXmlOutput()).build()
                    else -> shape
                }
            }
    }
}
