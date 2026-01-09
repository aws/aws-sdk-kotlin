/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.testutil.lines
import aws.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AwsQueryModeCustomizationTest {
    private val queryCompatibleModel = """
        namespace com.test

        use aws.protocols#awsJson1_0
        use aws.protocols#awsQueryCompatible
        use aws.api#service

        @awsJson1_0
        @awsQueryCompatible
        @service(sdkId: "QueryCompatible")
        service QueryCompatible {
            version: "1.0.0",
            operations: [GetFoo]
        }

        @http(method: "POST", uri: "/foo")
        operation GetFoo {
            input: GetFooInput
        }
        
        structure GetFooInput {
            payload: String
        }            
    """
        .trimIndent()
        .toSmithyModel()

    private val nonQueryCompatibleModel = """
        namespace com.test

        use aws.protocols#awsJson1_0
        use aws.protocols#awsQueryCompatible
        use aws.api#service

        @awsJson1_0
        @service(sdkId: "NonQueryCompatible")
        service NonQueryCompatible {
            version: "1.0.0",
            operations: [GetFoo]
        }

        @http(method: "POST", uri: "/foo")
        operation GetFoo {
            input: GetFooInput
        }
        
        structure GetFooInput {
            payload: String
        }            
    """
        .trimIndent()
        .toSmithyModel()

    @Test
    fun testEnabledForQueryCompatibleModel() {
        assertTrue {
            AwsQueryModeCustomization()
                .enabledForService(queryCompatibleModel, queryCompatibleModel.defaultSettings())
        }
    }

    @Test
    fun testNotExpectedForNonQueryCompatibleModel() {
        assertFalse {
            AwsQueryModeCustomization()
                .enabledForService(nonQueryCompatibleModel, nonQueryCompatibleModel.defaultSettings())
        }
    }

    @Test
    fun `x-amzn-query-mode applied`() {
        val ctx = queryCompatibleModel.newTestContext("QueryCompatible", integrations = listOf(AwsQueryModeCustomization()))
        val generator = MockHttpProtocolGenerator(queryCompatibleModel)
        generator.generateProtocolClient(ctx.generationCtx)

        ctx.generationCtx.delegator.finalize()
        ctx.generationCtx.delegator.flushWriters()

        val actual = ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")

        val getFooMethod = actual.lines("    override suspend fun getFoo(input: GetFooRequest): GetFooResponse {", "    }")

        val expectedHeaderMutation = """
            op.install(
                MutateHeaders().apply {
                    append("x-amzn-query-mode", "true")
                }
            )
        """.replaceIndent("        ")

        getFooMethod.shouldContainOnlyOnceWithDiff(expectedHeaderMutation)
    }

    @Test
    fun `x-amzn-query-mode NOT applied`() {
        val ctx = nonQueryCompatibleModel.newTestContext("NonQueryCompatible", integrations = listOf())
        val generator = MockHttpProtocolGenerator(nonQueryCompatibleModel)
        generator.generateProtocolClient(ctx.generationCtx)

        ctx.generationCtx.delegator.finalize()
        ctx.generationCtx.delegator.flushWriters()

        val actual = ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")

        val getFooMethod = actual.lines("    override suspend fun getFoo(input: GetFooRequest): GetFooResponse {", "    }")

        val unexpectedHeaderMutation = """
            op.install(
                MutateHeaders().apply {
                    append("x-amzn-query-mode", "true")
                }
            )
        """.replaceIndent("        ")

        getFooMethod.shouldNotContainOnlyOnceWithDiff(unexpectedHeaderMutation)
    }
}
