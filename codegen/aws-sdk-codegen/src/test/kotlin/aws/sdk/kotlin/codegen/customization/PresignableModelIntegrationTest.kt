/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.model.traits.Presignable
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.OperationShape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresignableModelIntegrationTest {
    private val testModel = """
            namespace smithy.example

            use aws.protocols#awsJson1_0
            use aws.auth#sigv4

            @awsJson1_0
            @sigv4(name: "example-signing-name")
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {
                input: GetFooInput
            }
            
            operation GetNotFoo {
                input: GetFooInput
            }
            
            structure GetFooInput {
                payload: String
            }            
        """.toSmithyModel()
    private val testContext = testModel.newTestContext("Example", "smithy.example")
    private val testPresignerModel = mapOf("smithy.example#Example" to setOf("smithy.example#GetFoo"))

    @Test
    fun testServiceEnablementInclusion() {
        val unit = PresignableModelIntegration(testPresignerModel)

        assertTrue(unit.enabledForService(testModel, testContext.generationCtx.settings))
    }

    @Test
    fun testServiceEnablementExclusion() {
        val testPresignerModel = mapOf("smithy.example#NoExample" to setOf("smithy.example#GetFoo"))

        val unit = PresignableModelIntegration(testPresignerModel)

        assertFalse(unit.enabledForService(testModel, testContext.generationCtx.settings))
    }

    @Test
    fun testModelMutation() {
        val testPresignerModel = mapOf("smithy.example#Example" to setOf("smithy.example#GetFoo"))

        val unit = PresignableModelIntegration(testPresignerModel)

        val model = unit.preprocessModel(testModel, testContext.generationCtx.settings)

        assertTrue(model.expectShape<OperationShape>("smithy.example#GetFoo").hasTrait(Presignable.ID))
        assertFalse(model.expectShape<OperationShape>("smithy.example#GetNotFoo").hasTrait(Presignable.ID))
    }
}
