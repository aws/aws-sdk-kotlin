/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.Presignable
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import aws.smithy.kotlin.codegen.test.toCodegenContext
import aws.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.model.shapes.OperationShape
import kotlin.test.Test
import kotlin.test.assertTrue

class PresignerGeneratorTest {
    private val testModel = """
            namespace smithy.kotlin.traits

            use aws.protocols#awsJson1_0
            use aws.auth#sigv4
            use aws.api#service
            use smithy.rules#endpointRuleSet

            @trait(selector: "*")
            structure presignable { }
            
            @awsJson1_0
            @sigv4(name: "example-signing-name")
            @service(sdkId: "example")
            @endpointRuleSet(
                version: "1.0",
                parameters: {},
                rules: [
                    {
                        "type": "endpoint",
                        "conditions": [],
                        "endpoint": {
                            "url": "https://static.endpoint.test"
                        }
                    }
                ]
            )
            service Example {
                version: "1.0.0",
                operations: [GetFoo, PostFoo, PutFoo]
            }

            @presignable
            @http(method: "POST", uri: "/foo")
            operation PostFoo {
                input: GetFooInput
            }
            
            @presignable
            @readonly
            @http(method: "GET", uri: "/foo")
            operation GetFoo { }
            
            @presignable
            @http(method: "PUT", uri: "/foo")
            @idempotent
            operation PutFoo {
                input: GetFooInput
            }

            structure GetFooInput {
                payload: String
            }
        """.toSmithyModel()
    private val testContext = testModel.newTestContext("Example", "smithy.kotlin.traits")

    @Test
    fun testCustomTraitOnModel() {
        assertTrue(testModel.expectShape<OperationShape>("smithy.kotlin.traits#GetFoo").hasTrait(Presignable.ID))
    }

    @Test
    fun testPresignerCodegen() {
        val unit = PresignerGenerator()
        unit.writeAdditionalFiles(testContext.toCodegenContext(), testContext.generationCtx.delegator)

        testContext.generationCtx.delegator.flushWriters()
        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("src/main/kotlin/smithy/kotlin/traits/presigners/Presigners.kt")

        val expected = """
            /**
             * Presign a [GetFooRequest] using the configuration of this [TestClient].
             * @param input The [GetFooRequest] to presign
             * @param duration The amount of time from signing for which the request is valid
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignGetFoo(input: GetFooRequest, duration: Duration): HttpRequest =
                presignGetFoo(input) { expiresAfter = duration }
        
            /**
             * Presign a [GetFooRequest] using the configuration of this [TestClient].
             * @param input The [GetFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignGetFoo(
                input: GetFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "GetFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = GetFooOperationSerializer().serialize(ctx, input)
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    configBlock()
                }
            }
        
            /**
             * Presign a [PostFooRequest] using the configuration of this [TestClient].
             * @param input The [PostFooRequest] to presign
             * @param duration The amount of time from signing for which the request is valid
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignPostFoo(input: PostFooRequest, duration: Duration): HttpRequest =
                presignPostFoo(input) { expiresAfter = duration }
        
            /**
             * Presign a [PostFooRequest] using the configuration of this [TestClient].
             * @param input The [PostFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignPostFoo(
                input: PostFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "PostFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = PostFooOperationSerializer().serialize(ctx, input)
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    configBlock()
                }
            }
        
            /**
             * Presign a [PutFooRequest] using the configuration of this [TestClient].
             * @param input The [PutFooRequest] to presign
             * @param duration The amount of time from signing for which the request is valid
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignPutFoo(input: PutFooRequest, duration: Duration): HttpRequest =
                presignPutFoo(input) { expiresAfter = duration }
        
            /**
             * Presign a [PutFooRequest] using the configuration of this [TestClient].
             * @param input The [PutFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignPutFoo(
                input: PutFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "PutFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = PutFooOperationSerializer().serialize(ctx, input)
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    configBlock()
                }
            }
        """.trimIndent()
        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignerCodegenAwsQuery() {
        val awsQueryModel = """
            namespace smithy.kotlin.traits

            use aws.protocols#awsQuery
            use aws.auth#sigv4
            use aws.api#service
            use smithy.rules#endpointRuleSet

            @trait(selector: "*")
            structure presignable { }
            
            @awsQuery
            @xmlNamespace(uri: "http://foo.com")
            @sigv4(name: "example-signing-name")
            @service(sdkId: "example")
            @endpointRuleSet(
                version: "1.0",
                parameters: {},
                rules: [
                    {
                        "type": "endpoint",
                        "conditions": [],
                        "endpoint": {
                            "url": "https://static.endpoint.test"
                        }
                    }
                ]
            )
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }
            
            @presignable
            @readonly
            @http(method: "GET", uri: "/foo")
            operation GetFoo { }
        """.toSmithyModel()
        val awsQueryContext = awsQueryModel.newTestContext("Example", "smithy.kotlin.traits")

        val unit = PresignerGenerator()
        unit.writeAdditionalFiles(awsQueryContext.toCodegenContext(), awsQueryContext.generationCtx.delegator)

        awsQueryContext.generationCtx.delegator.flushWriters()
        val awsQueryManifest = awsQueryContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = awsQueryManifest.expectFileString("src/main/kotlin/smithy/kotlin/traits/presigners/Presigners.kt")

        val expected = """
            /**
             * Presign a [GetFooRequest] using the configuration of this [TestClient].
             * @param input The [GetFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignGetFoo(
                input: GetFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "GetFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = GetFooOperationSerializer().serialize(ctx, input)
                unsignedRequest.method = HttpMethod.GET
                unsignedRequest.body.toByteStream()?.decodeToString()?.let {
                    val bodyParams = QueryParameters.parseEncoded(it)
                    unsignedRequest.url.parameters.addAll(bodyParams)
                }
            
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    hashSpecification = HashSpecification.CalculateFromPayload
                    configBlock()
                }
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignerForResourceOperation() {
        val awsQueryModel = """
            ${'$'}version: "2"
            namespace smithy.kotlin.traits

            use aws.protocols#awsJson1_0
            use aws.auth#sigv4
            use aws.api#service
            use smithy.rules#endpointRuleSet

            @trait(selector: "*")
            structure presignable { }
            
            @awsJson1_0
            @sigv4(name: "example-signing-name")
            @service(sdkId: "example")
            @endpointRuleSet(
                version: "1.0",
                parameters: {},
                rules: [
                    {
                        "type": "endpoint",
                        "conditions": [],
                        "endpoint": {
                            "url": "https://static.endpoint.test"
                        }
                    }
                ]
            )
            service Example {
                version: "1.0.0",
                resources: [ Foo ],
            }
            
            resource Foo {
                identifiers: { id: String },
                create: CreateFoo,
                delete: DeleteFoo,
                read: GetFoo,
            }

            @presignable
            @http(method: "POST", uri: "/foo")
            @idempotent
            operation DeleteFoo {
                input := {
                    @required
                    id: String
                }
            }
            
            @presignable
            @readonly
            @http(method: "GET", uri: "/foo")
            operation GetFoo {
                input := {
                    @httpQuery("id")
                    @required
                    id: String
                }
            }
            
            @presignable
            @http(method: "PUT", uri: "/foo")
            @idempotent
            operation CreateFoo { }
        """.toSmithyModel()
        val awsQueryContext = awsQueryModel.newTestContext("Example", "smithy.kotlin.traits")

        val unit = PresignerGenerator()
        unit.writeAdditionalFiles(awsQueryContext.toCodegenContext(), awsQueryContext.generationCtx.delegator)

        awsQueryContext.generationCtx.delegator.flushWriters()
        val awsQueryManifest = awsQueryContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = awsQueryManifest.expectFileString("src/main/kotlin/smithy/kotlin/traits/presigners/Presigners.kt")

        val expected = """
            /**
             * Presign a [CreateFooRequest] using the configuration of this [TestClient].
             * @param input The [CreateFooRequest] to presign
             * @param duration The amount of time from signing for which the request is valid
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignCreateFoo(input: CreateFooRequest, duration: Duration): HttpRequest =
                presignCreateFoo(input) { expiresAfter = duration }
        
            /**
             * Presign a [CreateFooRequest] using the configuration of this [TestClient].
             * @param input The [CreateFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignCreateFoo(
                input: CreateFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "CreateFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = CreateFooOperationSerializer().serialize(ctx, input)
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    configBlock()
                }
            }
        
            /**
             * Presign a [DeleteFooRequest] using the configuration of this [TestClient].
             * @param input The [DeleteFooRequest] to presign
             * @param duration The amount of time from signing for which the request is valid
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignDeleteFoo(input: DeleteFooRequest, duration: Duration): HttpRequest =
                presignDeleteFoo(input) { expiresAfter = duration }
        
            /**
             * Presign a [DeleteFooRequest] using the configuration of this [TestClient].
             * @param input The [DeleteFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignDeleteFoo(
                input: DeleteFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "DeleteFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = DeleteFooOperationSerializer().serialize(ctx, input)
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    configBlock()
                }
            }
        
            /**
             * Presign a [GetFooRequest] using the configuration of this [TestClient].
             * @param input The [GetFooRequest] to presign
             * @param duration The amount of time from signing for which the request is valid
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignGetFoo(input: GetFooRequest, duration: Duration): HttpRequest =
                presignGetFoo(input) { expiresAfter = duration }
        
            /**
             * Presign a [GetFooRequest] using the configuration of this [TestClient].
             * @param input The [GetFooRequest] to presign
             * @param signer The specific implementation of AWS signer to use. Defaults to DefaultAwsSigner.
             * @param configBlock A builder block for setting custom signing parameters. At a minimum the
             * [expiresAfter] field must be set.
             * @return An [HttpRequest] which can be invoked within the specified time window
             */
            public suspend fun TestClient.presignGetFoo(
                input: GetFooRequest,
                signer: AwsSigner = DefaultAwsSigner,
                configBlock: AwsSigningConfig.Builder.() -> Unit,
            ): HttpRequest {
                val ctx = ExecutionContext().apply {
                    set(SdkClientOption.OperationName, "GetFoo")
                    set(HttpOperationContext.OperationInput, input)
                }
                val unsignedRequest = GetFooOperationSerializer().serialize(ctx, input)
                val endpointResolver = EndpointResolverAdapter(config)
        
                return presignRequest(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {
                    if (service == null) service = "example-signing-name"
                    if (region == null) region = config.region
                    configBlock()
                }
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }
}
