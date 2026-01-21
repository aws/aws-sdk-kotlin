/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.ServiceClientConfigGenerator
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import aws.smithy.kotlin.codegen.test.toRenderingContext
import aws.smithy.kotlin.codegen.test.toSmithyModel
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.shapes.ServiceShape

class AwsServiceConfigIntegrationTest {
    @Test
    fun testServiceConfigurationProperties() {
        val model = """
            namespace com.test

            use aws.protocols#awsJson1_1
            use aws.api#service
            use aws.auth#sigv4

            @service(sdkId: "service with overrides", endpointPrefix: "service-with-overrides")
            @sigv4(name: "example")
            @awsJson1_1
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {}
        """.toSmithyModel()

        val serviceShape = model.expectShape<ServiceShape>("com.test#Example")

        val testCtx = model.newTestContext(serviceName = "Example")
        val writer = KotlinWriter("com.test")

        val renderingCtx = testCtx.toRenderingContext(writer, serviceShape)
            .copy(integrations = listOf(AwsServiceConfigIntegration()))

        ServiceClientConfigGenerator(serviceShape, detectDefaultProps = false).render(renderingCtx, renderingCtx.writer)
        val contents = writer.toString()

        val expectedProps = """
    override val region: String? = (builder.region ?: runBlocking { builder.regionProvider?.getRegion() ?: resolveRegion() })?.let { validateRegion(it) }
    override val regionProvider: RegionProvider = builder.regionProvider ?: DefaultRegionProviderChain()
    override val credentialsProvider: CredentialsProvider = builder.credentialsProvider ?: DefaultChainCredentialsProvider(httpClient = httpClient, region = region).manage()
"""
        contents.shouldContainOnlyOnceWithDiff(expectedProps)

        val expectedImpl = """
        /**
         * The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
         * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more information.
         * When specified, this static region configuration takes precedence over other region resolution methods.
         *
         * The region resolution order is:
         * 1. Static region (if specified)
         * 2. Custom region provider (if configured)
         * 3. Default region provider chain
         */
        override var region: String? = null

        /**
         * An optional region provider that determines the AWS region for client operations. When specified, this provider
         * takes precedence over the default region provider chain, unless a static region is explicitly configured.
         *
         * The region resolution order is:
         * 1. Static region (if specified)
         * 2. Custom region provider (if configured)
         * 3. Default region provider chain
         */
        override var regionProvider: RegionProvider? = null

        /**
         * The AWS credentials provider to use for authenticating requests. If not provided a
         * [aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider] instance will be used.
         * NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
         * client will not close it when the client is closed.
         */
        override var credentialsProvider: CredentialsProvider? = null
"""
        contents.shouldContainOnlyOnceWithDiff(expectedImpl)
    }
}
