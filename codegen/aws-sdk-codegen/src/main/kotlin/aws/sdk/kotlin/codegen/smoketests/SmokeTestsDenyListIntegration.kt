/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.smoketests

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestSectionIds.SmokeTestsFile
import software.amazon.smithy.model.Model

/**
 * SDK ID's of services that are deny listed from smoke test code generation.
 */
val smokeTestDenyList = setOf(
    "S3Tables",
)

/**
 * Will wipe the smoke test runner file for services that are deny listed.
 * Some services model smoke tests incorrectly and the code generated file will not compile.
 */
class SmokeTestsDenyListIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        settings.sdkId in smokeTestDenyList

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                SmokeTestsFile,
            ) { writer, _ ->
                writer.write("// Smoke tests for service are deny listed")
            },
        )
}
