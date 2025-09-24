/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.endpoints

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.test.*
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType
import kotlin.test.Test
import kotlin.test.assertTrue

class BindAwsEndpointBuiltinsTest {
    @Test
    fun testRenderAccountIdEndpointModeBuiltin() {
        val model = "".prependNamespaceAndService().toSmithyModel()

        val ctx = model.newTestContext().generationCtx
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        val parameters = listOf(
            Parameter
                .builder()
                .builtIn(AwsBuiltins.ACCOUNT_ID_ENDPOINT_MODE)
                .type(ParameterType.STRING)
                .name("accountIdEndpointMode")
                .build(),
        )

        renderBindAwsBuiltins(
            ctx,
            writer,
            parameters,
        )

        assertTrue(
            writer
                .rawString()
                .contains("accountIdEndpointMode = config.accountIdEndpointMode.toString().lowercase()"),
        )
    }
}
