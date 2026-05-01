/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.swf

import aws.sdk.kotlin.codegen.testutil.lines
import aws.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwfLongPollingIntegrationTest {
    private val swfModel = """
        namespace com.test

        use aws.protocols#awsJson1_0
        use aws.api#service

        @awsJson1_0
        @service(sdkId: "SWF")
        service TestService {
            version: "1.0.0",
            operations: [PollForActivityTask, PollForDecisionTask, RegisterDomain]
        }

        @http(method: "POST", uri: "/PollForActivityTask")
        operation PollForActivityTask {
            input: PollForActivityTaskInput
            output: PollForActivityTaskOutput
        }
        structure PollForActivityTaskInput {}
        structure PollForActivityTaskOutput {}

        @http(method: "POST", uri: "/PollForDecisionTask")
        operation PollForDecisionTask {
            input: PollForDecisionTaskInput
            output: PollForDecisionTaskOutput
        }
        structure PollForDecisionTaskInput {}
        structure PollForDecisionTaskOutput {}

        @http(method: "POST", uri: "/RegisterDomain")
        operation RegisterDomain {
            input: RegisterDomainInput
            output: RegisterDomainOutput
        }
        structure RegisterDomainInput {}
        structure RegisterDomainOutput {}
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
    fun testEnabledForSwf() {
        assertTrue { SwfLongPollingIntegration().enabledForService(swfModel, swfModel.defaultSettings()) }
    }

    @Test
    fun testNotEnabledForOtherService() {
        assertFalse { SwfLongPollingIntegration().enabledForService(otherModel, otherModel.defaultSettings()) }
    }

    @Test
    fun testPollForActivityTaskIsLongPolling() {
        assertOperationHasLongPolling("pollForActivityTask", "PollForActivityTaskRequest", "PollForActivityTaskResponse")
    }

    @Test
    fun testPollForDecisionTaskIsLongPolling() {
        assertOperationHasLongPolling("pollForDecisionTask", "PollForDecisionTaskRequest", "PollForDecisionTaskResponse")
    }

    @Test
    fun testRegisterDomainIsNotLongPolling() {
        assertOperationNotLongPolling("registerDomain", "RegisterDomainRequest", "RegisterDomainResponse")
    }

    private fun generateClient(): String {
        val ctx = swfModel.newTestContext("TestService", integrations = listOf(SwfLongPollingIntegration()))
        val generator = MockHttpProtocolGenerator(swfModel)
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
