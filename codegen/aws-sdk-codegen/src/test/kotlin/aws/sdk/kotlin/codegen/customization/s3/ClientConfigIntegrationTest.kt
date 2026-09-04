/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.testutil.model
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.rendering.ServiceClientConfigGenerator
import aws.smithy.kotlin.codegen.test.defaultSettings
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import aws.smithy.kotlin.codegen.test.toRenderingContext
import software.amazon.smithy.model.shapes.ServiceShape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientConfigIntegrationTest {
    @Test
    fun testEnabledForS3() {
        val model = model("S3")
        val enabled = ClientConfigIntegration().enabledForService(model, model.defaultSettings())
        assertTrue(enabled)
    }

    @Test
    fun testDisabledForNonS3Model() {
        val model = model("NotS3")
        val enabled = ClientConfigIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(enabled)
    }

    @Test
    fun testRendersPayloadSigningEnabledProperty() {
        val model = model("S3")
        val serviceShape = model.expectShape<ServiceShape>("com.test#S3")

        val testCtx = model.newTestContext("S3")
        val writer = KotlinWriter("com.test")

        val renderingCtx = testCtx.toRenderingContext(writer, serviceShape)
            .copy(integrations = listOf(ClientConfigIntegration()))

        ServiceClientConfigGenerator(serviceShape, detectDefaultProps = false).render(renderingCtx, renderingCtx.writer)
        val contents = writer.toString()

        // the immutable config property defaults to false
        contents.shouldContainOnlyOnceWithDiff("public val payloadSigningEnabled: Boolean = builder.payloadSigningEnabled ?: false")

        // the builder property is nullable with no default
        contents.shouldContainOnlyOnceWithDiff("public var payloadSigningEnabled: Boolean? = null")
    }
}
