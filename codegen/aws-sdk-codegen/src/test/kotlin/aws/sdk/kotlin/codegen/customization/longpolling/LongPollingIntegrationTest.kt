/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.longpolling

import aws.sdk.kotlin.codegen.testutil.lines
import aws.smithy.kotlin.codegen.test.*
import software.amazon.smithy.model.Model
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongPollingIntegrationTest {
    private val sqsModel = """
        namespace com.test

        use aws.protocols#awsJson1_0
        use aws.api#service

        @awsJson1_0
        @service(sdkId: "SQS")
        service TestService {
            version: "1.0.0",
            operations: [ReceiveMessage, SendMessage]
        }

        @http(method: "POST", uri: "/ReceiveMessage")
        operation ReceiveMessage {
            input: ReceiveMessageInput
            output: ReceiveMessageOutput
        }
        structure ReceiveMessageInput {}
        structure ReceiveMessageOutput {}

        @http(method: "POST", uri: "/SendMessage")
        operation SendMessage {
            input: SendMessageInput
            output: SendMessageOutput
        }
        structure SendMessageInput {}
        structure SendMessageOutput {}
    """.trimIndent().toSmithyModel()

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

    // enabledForService tests

    @Test
    fun testEnabledForSqs() {
        assertTrue { LongPollingIntegration().enabledForService(sqsModel, sqsModel.defaultSettings()) }
    }

    @Test
    fun testEnabledForSfn() {
        assertTrue { LongPollingIntegration().enabledForService(sfnModel, sfnModel.defaultSettings()) }
    }

    @Test
    fun testEnabledForSwf() {
        assertTrue { LongPollingIntegration().enabledForService(swfModel, swfModel.defaultSettings()) }
    }

    @Test
    fun testNotEnabledForOtherService() {
        assertFalse { LongPollingIntegration().enabledForService(otherModel, otherModel.defaultSettings()) }
    }

    // SQS: ReceiveMessage is long-polling, SendMessage is not

    @Test
    fun testSqsReceiveMessageIsLongPolling() {
        assertOperationHasLongPolling(sqsModel, "receiveMessage", "ReceiveMessageRequest", "ReceiveMessageResponse")
    }

    @Test
    fun testSqsSendMessageIsNotLongPolling() {
        assertOperationNotLongPolling(sqsModel, "sendMessage", "SendMessageRequest", "SendMessageResponse")
    }

    // SFN: GetActivityTask is long-polling, StartExecution is not

    @Test
    fun testSfnGetActivityTaskIsLongPolling() {
        assertOperationHasLongPolling(sfnModel, "getActivityTask", "GetActivityTaskRequest", "GetActivityTaskResponse")
    }

    @Test
    fun testSfnStartExecutionIsNotLongPolling() {
        assertOperationNotLongPolling(sfnModel, "startExecution", "StartExecutionRequest", "StartExecutionResponse")
    }

    // SWF: PollForActivityTask and PollForDecisionTask are long-polling, RegisterDomain is not

    @Test
    fun testSwfPollForActivityTaskIsLongPolling() {
        assertOperationHasLongPolling(swfModel, "pollForActivityTask", "PollForActivityTaskRequest", "PollForActivityTaskResponse")
    }

    @Test
    fun testSwfPollForDecisionTaskIsLongPolling() {
        assertOperationHasLongPolling(swfModel, "pollForDecisionTask", "PollForDecisionTaskRequest", "PollForDecisionTaskResponse")
    }

    @Test
    fun testSwfRegisterDomainIsNotLongPolling() {
        assertOperationNotLongPolling(swfModel, "registerDomain", "RegisterDomainRequest", "RegisterDomainResponse")
    }

    private fun generateClient(model: Model): String {
        val ctx = model.newTestContext("TestService", integrations = listOf(LongPollingIntegration()))
        val generator = MockHttpProtocolGenerator(model)
        generator.generateProtocolClient(ctx.generationCtx)
        ctx.generationCtx.delegator.finalize()
        ctx.generationCtx.delegator.flushWriters()
        return ctx.manifest.expectFileString("/src/main/kotlin/com/test/DefaultTestClient.kt")
    }

    private fun assertOperationHasLongPolling(model: Model, methodName: String, inputType: String, outputType: String) {
        val code = generateClient(model)
        val method = code.lines(
            "    override suspend fun $methodName(input: $inputType): $outputType {",
            "    }",
        )
        assertTrue(method.contains("op.context[HttpOperationContext.LongPolling] = true"), "Expected LongPolling set for $methodName")
    }

    private fun assertOperationNotLongPolling(model: Model, methodName: String, inputType: String, outputType: String) {
        val code = generateClient(model)
        val method = code.lines(
            "    override suspend fun $methodName(input: $inputType): $outputType {",
            "    }",
        )
        assertFalse(method.contains("HttpOperationContext.LongPolling"), "LongPolling should not be set for $methodName")
    }
}
