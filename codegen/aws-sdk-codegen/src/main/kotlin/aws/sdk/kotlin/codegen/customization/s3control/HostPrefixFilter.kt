/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3control

import aws.sdk.kotlin.codegen.customization.s3.isS3Control
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.EndpointTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Model customization to remove account ID host prefixes, which are now handled by endpoint resolution.
 */
class HostPrefixFilter : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3Control

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()
        return transformer.removeTraitsIf(model) { _, trait ->
            trait is EndpointTrait &&
                trait.hostPrefix.labels.any {
                    it.isLabel && it.content == "AccountId"
                }
        }
    }
}
