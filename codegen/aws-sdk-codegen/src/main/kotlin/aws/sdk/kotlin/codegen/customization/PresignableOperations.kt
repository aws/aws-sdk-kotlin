/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.model.traits.Presignable
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.transform.ModelTransformer

// The specification for which service operations are presignable
internal val DEFAULT_PRESIGNABLE_OPERATIONS: Map<String, Set<String>> = mapOf(
    "com.amazonaws.s3#AmazonS3" to setOf(
        "com.amazonaws.s3#GetObject",
        "com.amazonaws.s3#PutObject",
        "com.amazonaws.s3#UploadPart",
        "com.amazonaws.s3#DeleteObject",
    ),
    "com.amazonaws.sts#AWSSecurityTokenServiceV20110615" to setOf(
        "com.amazonaws.sts#GetCallerIdentity",
    ),
    "com.amazonaws.polly#Parrot_v1" to setOf(
        "com.amazonaws.polly#SynthesizeSpeech",
    ),
)

/**
 * This integration applies a custom trait to any AWS service that provides presign capability on one or more operations.
 */
class PresignableModelIntegration(private val presignedOperations: Map<String, Set<String>> = DEFAULT_PRESIGNABLE_OPERATIONS) : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val currentServiceId = model.expectShape<ServiceShape>(settings.service).id.toString()

        return presignedOperations.keys.contains(currentServiceId)
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val currentServiceId = model.expectShape<ServiceShape>(settings.service).id.toString()
        val presignedOperationIds = presignedOperations[currentServiceId]
            ?: error("Expected operation id for service $currentServiceId, but none found in $presignedOperations")
        val transformer = ModelTransformer.create()

        return transformer.mapShapes(model) { shape ->
            if (presignedOperationIds.contains(shape.id.toString())) {
                shape.asOperationShape().get().toBuilder().addTrait(Presignable()).build()
            } else {
                shape
            }
        }
    }
}
