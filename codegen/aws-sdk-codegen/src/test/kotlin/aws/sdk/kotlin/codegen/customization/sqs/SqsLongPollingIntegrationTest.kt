/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.sqs

import aws.sdk.kotlin.codegen.testutil.lines
import aws.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqsLongPollingIntegrationTest {
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
    fun testEnabledForSqs() {
        assertTrue { SqsLongPollingIntegration().enabledForService(sqsModel, sqsModel.defaultSettings()) }
    }

    @Test
    fun testNotEnabledForOtherService() {
        assertFalse { SqsLongPollingIntegration().enabledForService(otherModel, otherModel.defaultSettings()) }
    }

    @Test
    fun testReceiveMessageIsLongPolling() {
        assertOperationHasLongPolling("receiveMessage", "ReceiveMessageRequest", "ReceiveMessageResponse")
    }

    @Test
    fun testSendMessageIsNotLongPolling() {
        assertOperationNotLongPolling("sendMessage", "SendMessageRequest", "SendMessageResponse")
    }

    private fun generateClient(): String {
        val ctx = sqsModel.newTestContext("TestService", integrations = listOf(SqsLongPollingIntegration()))
        val generator = MockHttpProtocolGenerator(sqsModel)
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
