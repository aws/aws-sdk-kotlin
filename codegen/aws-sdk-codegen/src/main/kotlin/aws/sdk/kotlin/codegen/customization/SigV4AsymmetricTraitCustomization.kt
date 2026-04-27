/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.getTrait
import aws.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.transform.ModelTransformer

// FIXME: Remove services from customization or customization entirely when/if services add sigV4a trait to models
/**
 * Adds the sigV4A trait to services that don't model their sigV4A usage
 * NOTE: Won't add sigV4 trait (services that support sigV4A MUST support sigV4)
 */
class SigV4AsymmetricTraitCustomization : KotlinIntegration {
    // Needs to happen before the SigV4AsymmetricAuthSchemeIntegration & SigV4AuthSchemeIntegration
    override val order: Byte = -60

    // services which support SigV4A but don't model it
    private val unmodeledSigV4aServices = listOf("s3", "eventbridge", "sesv2")

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = unmodeledSigV4aServices.contains(settings.sdkId.lowercase()) && !model.isTraitApplied(SigV4ATrait::class.java)

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model = ModelTransformer.create().mapShapes(model) { shape ->
        when (shape.isServiceShape) {
            true -> {
                val builder = (shape as ServiceShape).toBuilder()

                if (!shape.hasTrait<SigV4ATrait>()) {
                    builder.addTrait(
                        SigV4ATrait.builder()
                            .name(shape.getTrait<SigV4Trait>()?.name ?: shape.getTrait<ServiceTrait>()?.arnNamespace)
                            .build(),
                    )
                }

                // SigV4A is added at the end because these services model SigV4A through endpoint rules instead of the service shape.
                // Because of that, SigV4A can apply to any operation, and the safest thing to do is add it at the end
                // and let the endpoint rules change priority as needed.
                val authTrait = shape.getTrait<AuthTrait>()?.let {
                    if (it.valueSet.contains(SigV4ATrait.ID)) {
                        it
                    } else {
                        AuthTrait(it.valueSet + mutableSetOf(SigV4ATrait.ID))
                    }
                } ?: AuthTrait(mutableSetOf(SigV4Trait.ID, SigV4ATrait.ID))
                builder.addTrait(authTrait)

                builder.build()
            }
            false -> shape
        }
    }
}
