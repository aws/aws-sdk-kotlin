/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.testutil.model
import aws.smithy.kotlin.codegen.test.defaultSettings
import aws.smithy.kotlin.codegen.test.newTestContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetObjectResponseLengthValidationIntegrationTest {
    @Test
    fun testNotExpectedForNonS3Model() {
        val model = model("NotS3")
        val actual = GetObjectResponseLengthValidationIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(actual)
    }

    @Test
    fun testExpectedForS3Model() {
        val model = model("S3")
        val actual = GetObjectResponseLengthValidationIntegration().enabledForService(model, model.defaultSettings())
        assertTrue(actual)
    }

    @Test
    fun testMiddlewareAddition() {
        val model = model("S3")
        val preexistingMiddleware = listOf(FooMiddleware)
        val ctx = model.newTestContext("S3")
        val actual = GetObjectResponseLengthValidationIntegration().customizeMiddleware(ctx.generationCtx, preexistingMiddleware)

        assertEquals(listOf(FooMiddleware, responseLengthValidationMiddleware), actual)
    }
}
