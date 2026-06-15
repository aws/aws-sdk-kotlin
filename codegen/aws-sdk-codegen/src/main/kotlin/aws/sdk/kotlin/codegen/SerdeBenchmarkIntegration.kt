/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinDelegator
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.core.defaultName
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.hasTrait
import aws.smithy.kotlin.codegen.rendering.ShapeValueGenerator
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointProviderGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.SerdeBenchmarkGeneratorFactory
import aws.smithy.kotlin.codegen.rendering.protocol.defaultUnboxedValue
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.IdempotencyTokenTrait
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait

class SerdeBenchmarkIntegration :
    KotlinIntegration,
    SerdeBenchmarkGeneratorFactory {

    private val generatedClassNames = mutableListOf<String>()

    override fun renderRequestBenchmark(
        ctx: ProtocolGenerator.GenerationContext,
        writer: KotlinWriter,
        operation: OperationShape,
        className: String,
        testCases: List<HttpRequestTestCase>,
    ) {
        generatedClassNames.add(className)
        HttpProtocolSerdeBenchmarkGenerator(ctx, writer, operation).renderRequestBenchmarkClass(className, testCases)
    }

    override fun renderResponseBenchmark(
        ctx: ProtocolGenerator.GenerationContext,
        writer: KotlinWriter,
        operation: OperationShape,
        className: String,
        testCases: List<HttpResponseTestCase>,
    ) {
        generatedClassNames.add(className)
        HttpProtocolSerdeBenchmarkGenerator(ctx, writer, operation).renderResponseBenchmarkClass(className, testCases)
    }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        if (generatedClassNames.isEmpty()) return

        delegator.useTestFileWriter("BenchmarkRegistration.kt", ctx.settings.pkg.name) { writer ->
            writer.withBlock("internal fun registerBenchmarks() {", "}") {
                for (className in generatedClassNames.sorted()) {
                    write("#T.register(#S) { #L() }", AwsRuntimeTypes.Benchmarks.BenchmarkRegistry, className, className)
                }
            }
        }
    }
}

private class HttpProtocolSerdeBenchmarkGenerator(
    private val ctx: ProtocolGenerator.GenerationContext,
    private val writer: KotlinWriter,
    private val operation: OperationShape,
) {
    private val model = ctx.model
    private val symbolProvider = ctx.symbolProvider
    private val serviceShape = ctx.service
    private val serviceSymbol = symbolProvider.toSymbol(serviceShape)
    private val opName = operation.defaultName()

    private val idempotentFieldsInModel: Boolean by lazy {
        operation.input.isPresent &&
            model.expectShape(operation.input.get()).members().any { it.hasTrait(IdempotencyTokenTrait.ID.name) }
    }

    fun renderRequestBenchmarkClass(className: String, testCases: List<HttpRequestTestCase>) {
        writer.write("")
        writer.withBlock("class #L : #T {", "}", className, AwsRuntimeTypes.Benchmarks.SerdeBenchmark) {
            write("")
            write("private val interceptor = #T()", AwsRuntimeTypes.Benchmarks.BenchmarkInterceptor)
            write("")

            withBlock("private val client = #T {", "}", serviceSymbol) {
                withBlock("httpClient = #T { _, request ->", "}", RuntimeTypes.HttpTest.TestEngine) {
                    if (isRpcV2Cbor()) {
                        write("val respHeaders = #T { append(#S, #S) }", RuntimeTypes.Http.Headers, "smithy-protocol", "rpc-v2-cbor")
                        write("val resp = #T(#T.OK, respHeaders, #T.Empty)", RuntimeTypes.Http.Response.HttpResponse, RuntimeTypes.Http.StatusCode, RuntimeTypes.Http.HttpBody)
                    } else {
                        write("val resp = #T(#T.OK, #T.Empty, #T.Empty)", RuntimeTypes.Http.Response.HttpResponse, RuntimeTypes.Http.StatusCode, RuntimeTypes.Http.Headers, RuntimeTypes.Http.HttpBody)
                    }
                    write("val now = #T.now()", RuntimeTypes.Core.Instant)
                    write("#T(request, resp, now, now, #T())", RuntimeTypes.Http.HttpCall, RuntimeTypes.HttpClient.Engine.callContext)
                }
                renderClientConfig()
                write("interceptors.add(interceptor)")
            }

            for (testCase in testCases) {
                renderInputField(testCase)
            }

            write("")
            withBlock("override suspend fun benchmarks(): #T<#T> {", "}", KotlinTypes.Collections.List, AwsRuntimeTypes.Benchmarks.BenchmarkResult) {
                write("val results = #T<#T>()", KotlinTypes.Collections.mutableListOf, AwsRuntimeTypes.Benchmarks.BenchmarkResult)
                for (testCase in testCases) {
                    val fieldName = "input_${sanitizeName(testCase.id)}"

                    withBlock("results.add(", ")") {
                        withBlock("#T.run(", ")", AwsRuntimeTypes.Benchmarks.BenchmarkHarness) {
                            write("id = #S,", testCase.id)
                            write("interceptor = interceptor,")
                            write("extractNanos = #T::serializationNanos,", AwsRuntimeTypes.Benchmarks.BenchmarkInterceptor)
                        }
                        write("{ client.#L(#L) }", opName, fieldName)
                    }
                }
                write("return results")
            }
        }

    }

    fun renderResponseBenchmarkClass(className: String, testCases: List<HttpResponseTestCase>) {
        writer.write("")
        writer.withBlock("class #L : #T {", "}", className, AwsRuntimeTypes.Benchmarks.SerdeBenchmark) {
            write("private val interceptor = #T()", AwsRuntimeTypes.Benchmarks.BenchmarkInterceptor)

            for (testCase in testCases) {
                renderResponseClientField(testCase)
            }

            if (operation.input.isPresent) {
                val inputShape = model.expectShape<StructureShape>(operation.input.get())
                val inputSymbol = symbolProvider.toSymbol(inputShape)
                val requiredMembers = inputShape.members().filter { it.isRequired }
                write("")
                withBlock("private val input = #T {", "}", inputSymbol) {
                    requiredMembers.forEach { member ->
                        val memberSymbol = symbolProvider.toSymbol(member)
                        val defaultValue = runCatching { memberSymbol.defaultUnboxedValue(this) }.getOrNull()
                        if (defaultValue != null) {
                            write("#L = #L", member.defaultName(), defaultValue)
                        }
                    }
                }
            }

            write("")
            withBlock("override suspend fun benchmarks(): #T<#T> {", "}", KotlinTypes.Collections.List, AwsRuntimeTypes.Benchmarks.BenchmarkResult) {
                write("val results = #T<#T>()", KotlinTypes.Collections.mutableListOf, AwsRuntimeTypes.Benchmarks.BenchmarkResult)
                for (testCase in testCases) {
                    val clientField = "client_${sanitizeName(testCase.id)}"
                    val inputArg = if (operation.input.isPresent) {
                        "input"
                    } else {
                        ""
                    }
                    withBlock("results.add(", ")") {
                        withBlock("#T.run(", ")", AwsRuntimeTypes.Benchmarks.BenchmarkHarness) {
                            write("id = #S,", testCase.id)
                            write("interceptor = interceptor,")
                            write("extractNanos = #T::deserializationNanos,", AwsRuntimeTypes.Benchmarks.BenchmarkInterceptor)
                        }
                        write("{ #L.#L(#L) }", clientField, opName, inputArg)
                    }
                }
                write("return results")
            }
        }

    }

    private fun renderClientConfig() {
        writer.write("region = #S", "us-east-1")
        writer.withBlock("credentialsProvider = #T {", "}", AwsRuntimeTypes.Config.Credentials.StaticCredentialsProvider) {
            write("accessKeyId = #S", "BENCHMARK")
            write("secretAccessKey = #S", "BENCHMARK")
        }
        if (idempotentFieldsInModel) {
            writer.write(
                "idempotencyTokenProvider = #T { #S }",
                RuntimeTypes.SmithyClient.IdempotencyTokenProvider,
                "00000000-0000-4000-8000-000000000000",
            )
        }
        if (!serviceShape.hasTrait<EndpointRuleSetTrait>()) {
            writer.write(
                "endpointProvider = #T { #T(#S) }",
                EndpointProviderGenerator.getSymbol(ctx.settings),
                RuntimeTypes.SmithyClient.Endpoints.Endpoint,
                "https://localhost",
            )
        }
    }

    private fun renderInputField(testCase: HttpRequestTestCase) {
        val fieldName = "input_${sanitizeName(testCase.id)}"

        if (operation.input.isPresent) {
            val inputShape = model.expectShape<StructureShape>(operation.input.get())
            writer.write("")
            writer.writeInline("private val #L = ", fieldName)
                .indent()
                .call {
                    ShapeValueGenerator(model, symbolProvider, explicitReceiver = true)
                        .instantiateShapeInline(writer, inputShape, testCase.params)
                }
                .dedent()
                .write("")
        }
    }

    private fun renderResponseClientField(testCase: HttpResponseTestCase) {
        val clientFieldName = "client_${sanitizeName(testCase.id)}"
        val bodyFieldName = "respBody_${sanitizeName(testCase.id)}"

        val body = testCase.body.orElse("").trim()
        writer.write("")
        if (body.isNotBlank()) {
            val isCborProtocol = testCase.protocol.name == "rpcv2Cbor"
            if (isCborProtocol) {
                writer.write(
                    "private val #L = #S.#T()",
                    bodyFieldName,
                    body,
                    RuntimeTypes.Core.Text.Encoding.decodeBase64Bytes,
                )
            } else {
                writer.write("private val #L = #S.encodeToByteArray()", bodyFieldName, body)
            }
        }

        writer.withBlock("private val #L = #T {", "}", clientFieldName, serviceSymbol) {
            withBlock("httpClient = #T { _, request ->", "}", RuntimeTypes.HttpTest.TestEngine) {
                if (testCase.headers.isNotEmpty()) {
                    withBlock("val respHeaders = #T {", "}", RuntimeTypes.Http.Headers) {
                        for ((key, value) in testCase.headers) {
                            write("append(#S, #S)", key, value)
                        }
                    }
                } else {
                    write("val respHeaders = #T.Empty", RuntimeTypes.Http.Headers)
                }

                if (body.isNotBlank()) {
                    write("val respBody = #T.fromBytes(#L)", RuntimeTypes.Http.HttpBody, bodyFieldName)
                } else {
                    write("val respBody = #T.Empty", RuntimeTypes.Http.HttpBody)
                }

                write("val resp = #T(#T.fromValue(#L), respHeaders, respBody)", RuntimeTypes.Http.Response.HttpResponse, RuntimeTypes.Http.StatusCode, testCase.code)
                write("val now = #T.now()", RuntimeTypes.Core.Instant)
                write("#T(request, resp, now, now, #T())", RuntimeTypes.Http.HttpCall, RuntimeTypes.HttpClient.Engine.callContext)
            }

            renderClientConfig()
            write("interceptors.add(interceptor)")
        }
    }

    private fun isRpcV2Cbor(): Boolean = serviceShape.allTraits.keys.any { it.toString().contains("rpcv2Cbor") }

    private fun sanitizeName(id: String): String = id.replace(Regex("[^a-zA-Z0-9_]"), "_")
        .replaceFirstChar { it.lowercaseChar() }
}
