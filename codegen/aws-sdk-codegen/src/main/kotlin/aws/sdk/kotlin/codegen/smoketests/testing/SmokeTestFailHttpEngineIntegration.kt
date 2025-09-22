/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.smoketests.testing

import aws.sdk.kotlin.codegen.model.traits.testing.TestFailedResponseTrait
import aws.sdk.kotlin.codegen.model.traits.testing.TestSuccessResponseTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestSectionIds
import software.amazon.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

/**
 * Adds [TestFailedResponseTrait] support to smoke tests
 * IMPORTANT: This integration is intended for use in integration or E2E tests only, not in real-life smoke tests that run
 * against a service endpoint.
 */
class SmokeTestFailHttpEngineIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.topDownOperations(settings.service).any { it.hasTrait<SmokeTestsTrait>() } &&
            !model.expectShape<ServiceShape>(settings.service).hasTrait(TestSuccessResponseTrait.ID) &&
            model.expectShape<ServiceShape>(settings.service).hasTrait(TestFailedResponseTrait.ID)

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(SmokeTestSectionIds.HttpEngineOverride, httpClientOverride),
        )

    private val httpClientOverride = SectionWriter { writer, _ ->
        writer.withBlock("httpClient = #T(", ")", RuntimeTypes.HttpTest.TestEngine) {
            withBlock("roundTripImpl = { _, request ->", "}") {
                write(
                    "val resp = #T(#T.BadRequest, #T.Empty, #T.Empty)",
                    RuntimeTypes.Http.Response.HttpResponse,
                    RuntimeTypes.Http.StatusCode,
                    RuntimeTypes.Http.Headers,
                    RuntimeTypes.Http.HttpBody,
                )
                write("val now = #T.now()", RuntimeTypes.Core.Instant)
                write("#T(request, resp, now, now)", RuntimeTypes.Http.HttpCall)
            }
        }
    }
}
