/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * Model customization to remove http-bound bucket from all operations, as this is now handled by endpoints resolution.
 */
class HttpPathFilter : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()
        return transformer.mapTraits(model) { _, trait ->
            when (trait) {
                is HttpTrait -> trait.withoutBucketInPath()
                else -> trait
            }
        }
    }
}

private fun HttpTrait.withoutBucketInPath(): HttpTrait =
    toBuilder()
        .uri(
            // the pattern builder doesn't let you give query params AND you still have to give both the stringified and
            // list versions of the path, so it's simpler to just round-trip a parse
            UriPattern.parse(
                uri.toString().replace(Regex("\\{Bucket}/?"), ""),
            ),
        )
        .build()
