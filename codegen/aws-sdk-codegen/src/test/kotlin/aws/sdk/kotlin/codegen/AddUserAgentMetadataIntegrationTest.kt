/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.testutil.lines
import aws.smithy.kotlin.codegen.test.*
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddUserAgentMetadataIntegrationTest {
    private val model = """
        @http(method: "GET", uri: "/foo")
        operation Foo { }
    """
        .trimIndent()
        .prependNamespaceAndService(operations = listOf("Foo"))
        .toSmithyModel()
    private val settings = model.defaultSettings()

    @Test
    fun testDisabled() {
        assertFalse(AddUserAgentMetadataIntegration().enabledForService(model, settings))
    }

    @Test
    fun testEnabledBySystemProperty() {
        withSysProp {
            assertTrue(AddUserAgentMetadataIntegration().enabledForService(model, settings))
        }
    }

    @Test
    fun testRenderProperty() {
        withSysProp {
            val ctx = model.newTestContext(integrations = listOf(AddUserAgentMetadataIntegration()))
            val generator = MockHttpProtocolGenerator(model)
            generator.generateProtocolClient(ctx.generationCtx)

            ctx.generationCtx.delegator.finalize()
            ctx.generationCtx.delegator.flushWriters()

            val actual = ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")
            val expected = """
                private val extraMetadata: Map<String, String> = mapOf(
                    "foo" to "bar",
                )
            """.replaceIndent("    ")
            actual.shouldContainOnlyOnceWithDiff(expected)
        }
    }

    @Test
    fun testRenderInterceptor() {
        withSysProp {
            val ctx = model.newTestContext(integrations = listOf(AddUserAgentMetadataIntegration()))
            val generator = MockHttpProtocolGenerator(model)
            generator.generateProtocolClient(ctx.generationCtx)

            ctx.generationCtx.delegator.finalize()
            ctx.generationCtx.delegator.flushWriters()

            val actual = ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")

            val fooMethod = actual.lines("    override suspend fun foo(input: FooRequest): FooResponse {", "    }")
            val expectedInterceptor = "op.interceptors.add(AddUserAgentMetadataInterceptor(extraMetadata))"
            fooMethod.shouldContainOnlyOnceWithDiff(expectedInterceptor)
        }
    }
}

private inline fun withSysProp(block: () -> Unit) {
    val path = Files.createTempFile(null, null)
    try {
        path.writeText(""" { "foo" : "bar" } """)
        System.setProperty(ADD_USER_AGENT_METADATA_SYS_PROP, path.toString())
        block()
    } finally {
        System.clearProperty(ADD_USER_AGENT_METADATA_SYS_PROP)
        path.deleteIfExists()
    }
}
