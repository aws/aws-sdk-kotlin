/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.toCodegenContext
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val model = """
         ${"$"}version: "2"
     
         namespace com.test
         
         use aws.api#service
     
         @service(sdkId: "Test")
         @title("Test Service")
         service Test {
             version: "1.0.0",
             operations: []
         }
     """.toSmithyModel()

val ctx = model.newTestContext("Test")

class ModuleDocumentationIntegrationTest {
    @Test
    fun integrationIsAppliedCorrectly() {
        val integration = ModuleDocumentationIntegration()
        assertTrue(integration.enabledForService(model, ctx.generationCtx.settings))

        integration.writeAdditionalFiles(ctx.toCodegenContext(), ctx.generationCtx.delegator)
        ctx.generationCtx.delegator.flushWriters()
        val testManifest = ctx.generationCtx.delegator.fileManifest as MockManifest

        val actual = testManifest.expectFileString("OVERVIEW.md")
        val expected = """
            # Module test

            This module contains the Kotlin SDK client for **Test Service**.
            
        """.trimIndent()

        assertEquals(expected, actual)
    }
}
