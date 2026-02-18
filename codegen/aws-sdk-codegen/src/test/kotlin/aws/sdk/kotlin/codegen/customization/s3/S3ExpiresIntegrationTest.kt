/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.testutil.model
import aws.smithy.kotlin.codegen.model.isDeprecated
import aws.smithy.kotlin.codegen.model.targetOrSelf
import aws.smithy.kotlin.codegen.test.defaultSettings
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.toSmithyModel
import aws.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.shapes.ShapeId
import kotlin.test.*
import kotlin.test.assertTrue

class S3ExpiresIntegrationTest {
    private val testModel = """
            namespace smithy.example

            use aws.protocols#restXml
            use aws.auth#sigv4
            use aws.api#service

            @restXml
            @sigv4(name: "s3")
            @service(
                sdkId: "S3"
                arnNamespace: "s3"
            )
            service S3 {
                version: "1.0.0",
                operations: [GetFoo, NewGetFoo]
            }

            operation GetFoo {
                input: GetFooInput
                output: GetFooOutput
            }
            
            operation NewGetFoo {
                input: GetFooInput
                output: NewGetFooOutput
            }
            
            structure GetFooInput {
                payload: String
                expires: String
            }   
                     
            @output
            structure GetFooOutput {
                expires: Timestamp
            }
            
            @output
            structure NewGetFooOutput {
                expires: String
            }
        """.toSmithyModel()

    @Test
    fun testEnabledForS3() {
        val enabled = S3ExpiresIntegration().enabledForService(testModel, testModel.defaultSettings())
        assertTrue(enabled)
    }

    @Test
    fun testDisabledForNonS3Model() {
        val model = model("NotS3")
        val enabled = S3ExpiresIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(enabled)
    }

    @Test
    fun testMiddlewareAddition() {
        val model = model("S3")
        val preexistingMiddleware = listOf(FooMiddleware)
        val ctx = model.newTestContext("S3")

        val integration = S3ExpiresIntegration()
        val actual = integration.customizeMiddleware(ctx.generationCtx, preexistingMiddleware)

        assertEquals(listOf(FooMiddleware, integration.applyExpiresFieldInterceptor), actual)
    }

    @Test
    fun testPreprocessModel() {
        val integration = S3ExpiresIntegration()
        val model = integration.preprocessModel(testModel, testModel.defaultSettings())

        val expiresShapes = listOf(
            model.expectShape(ShapeId.from("smithy.example#GetFooInput\$expires")),
            model.expectShape(ShapeId.from("smithy.example#GetFooOutput\$expires")),
            model.expectShape(ShapeId.from("smithy.example#NewGetFooOutput\$expires")),
        )
        // `Expires` members should always be Timestamp, even if its modeled as a string
        assertTrue(expiresShapes.all { it.targetOrSelf(model).isTimestampShape })

        // All `Expires` output members should be deprecated
        assertTrue(
            expiresShapes
                .filter { it.id.toString().contains("Output") }
                .all { it.isDeprecated },
        )

        val expiresStringFields = listOf(
            model.expectShape(ShapeId.from("smithy.example#GetFooOutput\$expiresString")),
            model.expectShape(ShapeId.from("smithy.example#NewGetFooOutput\$expiresString")),
        )
        // There should be no `ExpiresString` member added to the input shape
        assertNull(model.getShape(ShapeId.from("smithy.example#GetFooInput\$expiresString")).getOrNull())

        // There should be a synthetic `ExpiresString` string member added to output shapes
        assertTrue(expiresStringFields.all { it.targetOrSelf(model).isStringShape })

        // The synthetic fields should NOT be deprecated
        assertTrue(expiresStringFields.none { it.isDeprecated })
    }
}
