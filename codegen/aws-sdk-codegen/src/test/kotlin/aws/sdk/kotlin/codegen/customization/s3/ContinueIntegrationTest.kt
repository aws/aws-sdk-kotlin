/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.testutil.lines
import aws.sdk.kotlin.codegen.testutil.model
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.rendering.ServiceClientConfigGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.test.*
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.shapes.OperationShape
import kotlin.test.*

class ContinueIntegrationTest {
    @Test
    fun testNotExpectedForNonS3Model() {
        val model = model("NotS3")
        val actual = ContinueIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(actual)
    }

    @Test
    fun testExpectedForS3Model() {
        val model = model("S3")
        val actual = ContinueIntegration().enabledForService(model, model.defaultSettings())
        assertTrue(actual)
    }

    @Test
    fun testMiddlewareAddition() {
        val model = model("S3")
        val preexistingMiddleware = listOf(FooMiddleware)
        val ctx = model.newTestContext("S3")
        val actual = ContinueIntegration().customizeMiddleware(ctx.generationCtx, preexistingMiddleware)

        assertEquals(listOf(FooMiddleware, ContinueMiddleware), actual)
    }

    @Test
    fun testRenderConfigProperty() {
        val model = model("S3")
        val ctx = model.newTestContext("S3")
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        val serviceShape = model.serviceShapes.single()
        val renderingCtx = ctx
            .toRenderingContext(writer, serviceShape)
            .copy(integrations = listOf(ContinueIntegration()))

        val generator = ServiceClientConfigGenerator(serviceShape, detectDefaultProps = false)
        generator.render(renderingCtx, writer)
        val contents = writer.toString()

        val expectedImmutableProp = """
            public val continueHeaderThresholdBytes: Long? = builder.continueHeaderThresholdBytes
        """.trimIndent()
        contents.shouldContainOnlyOnceWithDiff(expectedImmutableProp)

        val expectedBuilderProp = """
            /**
             * The minimum content length threshold (in bytes) for which to send `Expect: 100-continue` HTTP headers. PUT
             * requests with bodies at or above this length will include this header, as will PUT requests with a null content
             * length. Defaults to 2 megabytes.
             *
             * This property may be set to `null` to disable sending the header regardless of content length.
             */
            public var continueHeaderThresholdBytes: Long? = 2 * 1024 * 1024 // 2MB
        """.replaceIndent("        ")
        contents.shouldContainOnlyOnceWithDiff(expectedBuilderProp)
    }

    @Test
    fun testRenderInterceptor() {
        val model = model("S3")
        val ctx = model.newTestContext("S3", integrations = listOf(ContinueIntegration()))
        val generator = MockHttpProtocolGenerator(model)
        generator.generateProtocolClient(ctx.generationCtx)

        ctx.generationCtx.delegator.finalize()
        ctx.generationCtx.delegator.flushWriters()

        val actual = ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")

        val fooMethod = actual.lines("    override suspend fun foo(input: FooRequest): FooResponse {", "    }")
        val expectedInterceptor = """
            config.continueHeaderThresholdBytes?.let { threshold ->
                op.interceptors.add(ContinueInterceptor(threshold))
            }
        """.replaceIndent("        ")
        fooMethod.shouldContainOnlyOnceWithDiff(expectedInterceptor)

        val barMethod = actual.lines("    override suspend fun bar(input: BarRequest): BarResponse {", "    }")
        barMethod.shouldNotContainOnlyOnceWithDiff(expectedInterceptor)
    }
}

object FooMiddleware : ProtocolMiddleware {
    override val name: String = "FooMiddleware"
    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) = fail("Unexpected call to `FooMiddleware.render")
}
