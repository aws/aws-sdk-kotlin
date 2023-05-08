/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientConfigGenerator
import software.amazon.smithy.kotlin.codegen.test.*
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
    override val region: String = requireNotNull(builder.region) { "region is a required configuration property" }
    override val credentialsProvider: CredentialsProvider = builder.credentialsProvider ?: DefaultChainCredentialsProvider(httpClientEngine = httpClientEngine, region = region).manage()
"""
        contents.shouldContainOnlyOnceWithDiff(expectedProps)

        val expectedImpl = """
        /**
         * The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
         * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more
         * information
         */
        override var region: String? = null

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
