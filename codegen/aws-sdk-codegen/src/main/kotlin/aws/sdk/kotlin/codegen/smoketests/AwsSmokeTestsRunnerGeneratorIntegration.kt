/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.smoketests

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.*
import software.amazon.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.Model
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

/**
 * Generates AWS specific code for smoke test runners
 */
class AwsSmokeTestsRunnerGeneratorIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.topDownOperations(settings.service).any { it.hasTrait<SmokeTestsTrait>() }

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            AwsSmokeTestsRunnerGenerator.regionEnvironmentVariable,
            AwsSmokeTestsRunnerGenerator.clientConfig,
            AwsSmokeTestsRunnerGenerator.defaultClientConfig,
            AwsSmokeTestsRunnerGenerator.skipTagsEnvironmentVariable,
            AwsSmokeTestsRunnerGenerator.serviceFilterEnvironmentVariable,
        )
}

/**
 * The section writer bindings used by [AwsSmokeTestsRunnerGeneratorIntegration]
 */
private object AwsSmokeTestsRunnerGenerator {
    /**
     * Adds region environment variable support to AWS smoke test runners.
     * Preserves other environment variables added via section writer binding, if any.
     */
    val regionEnvironmentVariable =
        SectionWriterBinding(SmokeTestSectionIds.AdditionalEnvironmentVariables) { writer, previous ->
            writer.write("#L", previous)
            writer.write(
                "private val regionOverride = #T.System.getenv(#S)",
                RuntimeTypes.Core.Utils.PlatformProvider,
                AWS_REGION,
            )
        }

    /**
     * Add AWS specific client config support to AWS smoke test runners
     */
    val clientConfig =
        SectionWriterBinding(SmokeTestSectionIds.ClientConfig) { writer, _ ->
            val name = writer.getContextValue(SmokeTestSectionIds.ClientConfig.Name)
            val value = writer.getContextValue(SmokeTestSectionIds.ClientConfig.Value)

            // Normalize client config names
            val newName = when (name) {
                "uri" -> "endpointProvider"
                "useDualstack" -> "useDualStack"
                "sigv4aRegionSet" -> "sigV4aSigningRegionSet"
                "useAccountIdRouting" -> "accountIdEndpointMode"
                "useAccelerate" -> "enableAccelerate"
                "useMultiRegionAccessPoints" -> "disableMrap"
                "useGlobalEndpoint" -> {
                    writer.write("throw #T(#S)", RuntimeTypes.Core.SmokeTests.SmokeTestsException, "'useGlobalEndpoint' is not supported by the SDK")
                    return@SectionWriterBinding
                }
                else -> name
            }
            writer.writeInline("#L = ", newName)

            // Normalize client values
            when (newName) {
                "endpointProvider" -> {
                    val endpointProvider = writer.getContextValue(SmokeTestSectionIds.ClientConfig.EndpointProvider)
                    val endpointParameters = writer.getContextValue(SmokeTestSectionIds.ClientConfig.EndpointParams)

                    writer.withBlock("object : #T {", "}", endpointProvider) {
                        write(
                            "override suspend fun resolveEndpoint(params: #1T): #2T = #2T(#3L)",
                            endpointParameters,
                            RuntimeTypes.SmithyClient.Endpoints.Endpoint,
                            value,
                        )
                    }
                }
                "sigV4aSigningRegionSet" -> {
                    // Render new value
                    writer.write("#L.toSet()", value)
                    // Also configure sigV4a - TODO: Remove once sigV4a is supported for default signer.
                    writer.write(
                        "authSchemes = listOf(#T(#T))",
                        RuntimeTypes.Auth.HttpAuthAws.SigV4AsymmetricAuthScheme,
                        RuntimeTypes.Auth.AwsSigningCrt.CrtAwsSigner,
                    )
                }
                "accountIdEndpointMode" -> {
                    when (value) {
                        "true" -> writer.write("#T.REQUIRED", AwsRuntimeTypes.Config.Endpoints.AccountIdEndpointMode)
                        "false" -> writer.write("#T.DISABLED", AwsRuntimeTypes.Config.Endpoints.AccountIdEndpointMode)
                    }
                }
                "disableMrap" -> {
                    when (value) {
                        "true" -> writer.write("false")
                        "false" -> writer.write("true")
                    }
                }
                "region" -> {
                    writer.write("regionOverride ?: #L", value)
                }
                else -> writer.write("#L", value)
            }
        }

    /**
     * Add default client config to AWS smoke test runners.
     * Preserves previous default config if any.
     */
    val defaultClientConfig =
        SectionWriterBinding(SmokeTestSectionIds.DefaultClientConfig) { writer, previous ->
            writer.write("#L", previous)
            writer.write("region = regionOverride")
        }

    /**
     * Replaces environment variable with one specific to AWS smoke test runners
     */
    val skipTagsEnvironmentVariable =
        SectionWriterBinding(SmokeTestSectionIds.SkipTags) { writer, _ -> writer.writeInline("#S", AWS_SKIP_TAGS) }

    /**
     * Replaces environment variable with one specific to AWS smoke test runners
     */
    val serviceFilterEnvironmentVariable =
        SectionWriterBinding(SmokeTestSectionIds.ServiceFilter) { writer, _ -> writer.writeInline("#S", AWS_SERVICE_FILTER) }
}

/**
 * Env var for AWS smoke test runners.
 * Should be a string that corresponds to an AWS region.
 * The region to use when executing smoke tests. This value MUST override any value supplied in the smoke tests themselves.
 */
private const val AWS_REGION = "AWS_SMOKE_TEST_REGION"

/**
 * Env var for AWS smoke test runners.
 * Should be a comma-delimited list of strings that correspond to tags on the test cases.
 * If a test case is tagged with one of the tags indicated by AWS_SMOKE_TEST_SKIP_TAGS, it MUST be skipped by the smoke test runner.
 */
const val AWS_SKIP_TAGS = "AWS_SMOKE_TEST_SKIP_TAGS"

/**
 * Env var for AWS smoke test runners.
 * Should be a comma-separated list of service identifiers to test.
 */
const val AWS_SERVICE_FILTER = "AWS_SMOKE_TEST_SERVICE_IDS"
