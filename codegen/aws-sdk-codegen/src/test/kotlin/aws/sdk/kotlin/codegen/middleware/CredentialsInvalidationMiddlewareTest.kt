/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.middleware

import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.test.TestModelDefault
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.prependNamespaceAndService
import aws.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.OperationShape
import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialsInvalidationMiddlewareTest {
    private val model = """
        @http(method: "POST", uri: "/modeled")
        operation ModeledExpiredToken {
            errors: [ExpiredTokenException]
        }

        @http(method: "POST", uri: "/query")
        operation QueryCodeExpiredToken {
            errors: [TokenExpiredError]
        }

        @http(method: "POST", uri: "/other")
        operation OtherErrors {
            errors: [ThrottledException]
        }

        @http(method: "POST", uri: "/none")
        operation NoErrors { }

        @error("client")
        structure ExpiredTokenException { }

        @error("client")
        @awsQueryError(code: "ExpiredTokenException", httpResponseCode: 400)
        structure TokenExpiredError { }

        @error("client")
        structure ThrottledException { }
    """
        .prependNamespaceAndService(
            imports = listOf("aws.protocols#awsQueryError"),
            operations = listOf("ModeledExpiredToken", "QueryCodeExpiredToken", "OtherErrors", "NoErrors"),
        )
        .toSmithyModel()

    private fun render(operation: String): String {
        val ctx = model.newTestContext()
        val op = model.expectShape<OperationShape>("${TestModelDefault.NAMESPACE}#$operation")
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        CredentialsInvalidationMiddleware().render(ctx.generationCtx, op, writer)
        return writer.toString().lines().last { it.isNotBlank() }
    }

    @Test
    fun testOperationModelingTheErrorPassesItsCode() {
        assertEquals("op.interceptors.add(CredentialsInvalidationInterceptor(setOf(\"ExpiredTokenException\")))", render("ModeledExpiredToken"))
    }

    @Test
    fun testTheAwsQueryErrorCodeIsTheWireCode() {
        assertEquals("op.interceptors.add(CredentialsInvalidationInterceptor(setOf(\"ExpiredTokenException\")))", render("QueryCodeExpiredToken"))
    }

    @Test
    fun testOperationsNotModelingTheErrorUseTheDefault() {
        assertEquals("op.interceptors.add(CredentialsInvalidationInterceptor())", render("OtherErrors"))
        assertEquals("op.interceptors.add(CredentialsInvalidationInterceptor())", render("NoErrors"))
    }
}
