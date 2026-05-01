/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.test.TestModelDefault
import aws.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import kotlin.test.*

class ServiceClientCompanionObjectWriterTest {
    @Test
    fun testBase() {
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        writer.putContext(
            mapOf(
                "ServiceSymbol" to buildSymbol { name = "TestGeneratorClient" },
                "SdkId" to "Test Generator",
            ),
        )
        ServiceClientCompanionObjectWriter().write(writer, null)

        val expected = """
            override suspend fun finalizeEnvironmentalConfig(builder: Builder, sharedConfig: LazyAsyncValue<AwsSharedConfig>, activeProfile: LazyAsyncValue<AwsProfile>) {
                super.finalizeEnvironmentalConfig(builder, sharedConfig, activeProfile)
                builder.config.endpointUrl = builder.config.endpointUrl ?: resolveEndpointUrl(
                    sharedConfig,
                    "TestGenerator",
                    "TEST_GENERATOR",
                    "test_generator",
                )
            }
        """.trimIndent()

        writer.toString().shouldContainOnlyOnceWithDiff(expected)
    }
}
