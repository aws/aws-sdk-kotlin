/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.sfn

import aws.sdk.kotlin.codegen.testutil.lines
import aws.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SfnLongPollingIntegrationTest {
    private val sfnModel = """
        namespace com.test

        use aws.protocols#awsJson1_0
        use aws.api#service

        @awsJson1_0
        @service(sdkId: "SFN")
        service TestService {
            version: "1.0.0",
            operations: [GetActivityTask, StartExecution]
        }

        @http(method: "POST", uri: "/GetActivityTask")
        operation GetActivityTask {
            input: GetActivityTaskInput
            output: GetActivityTaskOutput
        }
        structure GetActivityTaskInput {}
        structure GetActivityTaskOutput {}

        @http(method: "POST", uri: "/StartExecution")
        operation StartExecution {
            input: StartExecutionInput
            output: StartExecutionOutput
        }
        structure StartExecutionInput {}
        structure StartExecutionOutput {}
    """.trimIndent().toSmithyModel()

    private val otherModel = """
        namespace com.test

        use aws.protocols#awsJson1_0
        use aws.api#service

        @awsJson1_0
        @service(sdkId: "Other")
        service TestService {
            version: "1.0.0",
            operations: [DoSomething]
        }

        @http(method: "POST", uri: "/DoSomething")
        operation DoSomething {
            input: DoSomethingInput
            output: DoSomethingOutput
        }
        structure DoSomethingInput {}
        structure DoSomethingOutput {}
    """.trimIndent().toSmithyModel()

    @Test
    fun testEnabledForSfn() {
        assertTrue { SfnLongPollingIntegration().enabledForService(sfnModel, sfnModel.defaultSettings()) }
    }

    @Test
    fun testNotEnabledForOtherService() {
        assertFalse { SfnLongPollingIntegration().enabledForService(otherModel, otherModel.defaultSettings()) }
    }

    @Test
    fun testGetActivityTaskIsLongPolling() {
        assertOperationHasLongPolling("getActivityTask", "GetActivityTaskRequest", "GetActivityTaskResponse")
    }

    @Test
    fun testStartExecutionIsNotLongPolling() {
        assertOperationNotLongPolling("startExecution", "StartExecutionRequest", "StartExecutionResponse")
    }

    private fun generateClient(): String {
        val ctx = sfnModel.newTestContext("TestService", integrations = listOf(SfnLongPollingIntegration()))
        val generator = MockHttpProtocolGenerator(sfnModel)
        generator.generateProtocolClient(ctx.generationCtx)
        ctx.generationCtx.delegator.finalize()
        ctx.generationCtx.delegator.flushWriters()
        return ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")
    }

    private fun assertOperationHasLongPolling(methodName: String, inputType: String, outputType: String) {
        val code = generateClient()
        val method = code.lines(
            "    override suspend fun $methodName(input: $inputType): $outputType {",
            "    }",
        )
        assertTrue(method.contains("op.context[HttpOperationContext.LongPolling] = true"), "Expected LongPolling set for $methodName")
    }

    private fun assertOperationNotLongPolling(methodName: String, inputType: String, outputType: String) {
        val code = generateClient()
        val method = code.lines(
            "    override suspend fun $methodName(input: $inputType): $outputType {",
            "    }",
        )
        assertFalse(method.contains("HttpOperationContext.LongPolling"), "LongPolling should not be set for $methodName")
    }
}
