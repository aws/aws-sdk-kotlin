/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.cloudfrontkeyvaluestore

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Modify the [AuthTrait] of the CloudFront KeyValueStore service, placing SigV4A before SigV4 in the
 * prioritized list.
 */
class BackfillSigV4ACustomization : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = settings.sdkId.lowercase() == "cloudfront keyvaluestore"

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model =
        ModelTransformer.create().mapShapes(model) { shape ->
            when (shape.isServiceShape) {
                true -> {
                    val builder = (shape as ServiceShape).toBuilder()

                    builder.addTrait(
                        SigV4ATrait.builder()
                            .name(shape.getTrait<SigV4Trait>()?.name ?: shape.getTrait<ServiceTrait>()?.arnNamespace)
                            .build(),
                    )

                    val authTraitValueSet = shape.getTrait<AuthTrait>()?.valueSet ?: mutableSetOf()

                    if (authTraitValueSet.firstOrNull() != SigV4ATrait.ID) {
                        // remove existing auth trait
                        builder.removeTrait(AuthTrait.ID)

                        // add a new auth trait with SigV4A in front of the existing values
                        builder.addTrait(AuthTrait(mutableSetOf(SigV4ATrait.ID) + authTraitValueSet))
                    }

                    builder.build()
                }
                false -> shape
            }
        }
}
