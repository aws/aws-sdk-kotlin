/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.Presignable
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionId
import aws.smithy.kotlin.codegen.integration.SectionKey
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.*
import aws.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointResolverAdapterGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.HttpBindingProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import aws.smithy.kotlin.codegen.rendering.serde.serializerName
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Represents a presignable operation.
 * @property shape The [OperationShape] which is capable of presigning
 * @property signBody true if the body is to be read and signed, otherwise body specified as unsigned.
 * @property liftBodyToQueryString true if the body should be lifted to query string parameters and the request
 * converted to an HTTP `GET`
 */
data class PresignableOperation(val shape: OperationShape, val signBody: Boolean, val liftBodyToQueryString: Boolean)

/**
 * This integration applies to any AWS service that provides presign capability on one or more operations.
 */
class PresignerGenerator : KotlinIntegration {
    /**
     * Identifies the [UnsignedRequestCustomizationSection] section for custom handling of the unsigned request before
     * signing.
     */
    object UnsignedRequestCustomizationSection : SectionId {
        val OperationId: SectionKey<String> = SectionKey("OperationId")
        val HttpBindingResolver: SectionKey<HttpBindingResolver> = SectionKey("HttpBindingResolver")
        val DefaultTimestampFormat: SectionKey<TimestampFormatTrait.Format> = SectionKey("DefaultTimestampFormat")
    }

    /**
     * Identifies the [SigningConfigCustomizationSection] section customizing the AWS signing config.
     */
    object SigningConfigCustomizationSection : SectionId

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val protocolGenerator: HttpBindingProtocolGenerator = ctx.protocolGenerator as HttpBindingProtocolGenerator
        val httpBindingResolver = protocolGenerator.getProtocolHttpBindingResolver(ctx.model, service)
        val defaultTimestampFormat = protocolGenerator.defaultTimestampFormat

        // Only services with SigV4 are currently presignable
        val sig4vTrait = service.getTrait<SigV4Trait>() ?: return

        // Scan model for operations decorated with PresignTrait
        val presignOperations = TopDownIndex
            .of(ctx.model)
            .getContainedOperations(service)
            .filter { it.hasTrait(Presignable.ID) }
            .map { operationShape ->
                check(AwsSignatureVersion4.hasSigV4AuthScheme(ctx.model, service, operationShape)) {
                    "Operation does not have valid auth trait"
                }
                val signBody = signBody(protocolGenerator.protocol)
                val liftBodyToQueryString = liftBodyToQueryString(protocolGenerator.protocol)
                PresignableOperation(operationShape, signBody, liftBodyToQueryString)
            }

        // If presignable operations found for this service, generate a Presigner file
        if (presignOperations.isNotEmpty()) {
            delegator.useFileWriter("Presigners.kt", "${ctx.settings.pkg.name}.presigners") { writer ->
                renderPresigners(
                    writer,
                    ctx,
                    httpBindingResolver,
                    sig4vTrait.name,
                    presignOperations,
                    defaultTimestampFormat,
                )
            }
        }
    }

    // UNKNOWN Is this truly necessary for all AWS Query or merely STS?
    private fun liftBodyToQueryString(protocolShapeId: ShapeId) = protocolShapeId == AwsQueryTrait.ID

    // Determine if body should be read and signed by CRT.  If body is to be signed by CRT, null is passed to signer
    // for signedBodyValue parameter. This causes CRT to read the body and compute the signature.
    // Otherwise, AwsSignedBodyValue.UNSIGNED_PAYLOAD is passed specifying that the body will be ignored and CRT
    // will not take the body into account when signing the request.
    private fun signBody(protocolShapeId: ShapeId) = protocolShapeId == AwsQueryTrait.ID

    private fun renderPresigners(
        writer: KotlinWriter,
        ctx: CodegenContext,
        httpBindingResolver: HttpBindingResolver,
        sigv4ServiceName: String,
        presignOperations: List<PresignableOperation>,
        defaultTimestampFormat: TimestampFormatTrait.Format,
    ) {
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)

        // Iterate over each presignable operation and generate presign functions
        presignOperations.forEachIndexed { index, presignableOp ->
            val op = presignableOp.shape
            val request = ctx.model.expectShape<StructureShape>(op.input.get())
            val requestSymbol = ctx.symbolProvider.toSymbol(request)
            val serializerSymbol = buildSymbol {
                definitionFile = "${op.serializerName()}.kt"
                name = op.serializerName()
                namespace = ctx.settings.pkg.serde
            }

            val contextMap: Map<SectionKey<*>, Any> = mapOf(
                UnsignedRequestCustomizationSection.OperationId to op.id.toString(),
                UnsignedRequestCustomizationSection.HttpBindingResolver to httpBindingResolver,
                UnsignedRequestCustomizationSection.DefaultTimestampFormat to defaultTimestampFormat,
            )

            if (index != 0) writer.write("")

            // Generate the shorthand convenience presign extension function for service client
            renderShorthandPresignFn(writer, serviceSymbol, presignableOp, requestSymbol)

            // Generate the full presign extension function for service client
            renderFullPresignFn(
                ctx,
                writer,
                serviceShape,
                serviceSymbol,
                sigv4ServiceName,
                presignableOp,
                requestSymbol,
                serializerSymbol,
                contextMap,
            )
        }
    }

    private fun renderShorthandPresignFn(
        writer: KotlinWriter,
        serviceSymbol: Symbol,
        presignableOp: PresignableOperation,
        requestSymbol: Symbol,
    ) {
        writer.dokka {
            write("Presign a [#T] using the configuration of this [#T].", requestSymbol, serviceSymbol)
            write("@param input The [#T] to presign", requestSymbol)
            write("@param duration The amount of time from signing for which the request is valid")
            write(
                "@return An [#T] which can be invoked within the specified time window",
                RuntimeTypes.Http.Request.HttpRequest,
            )
        }

        writer
            .withBlock(
                "public suspend fun #T.presign#L(input: #T, duration: #T): #T =",
                "",
                serviceSymbol,
                presignableOp.shape.id.name,
                requestSymbol,
                KotlinTypes.Time.Duration,
                RuntimeTypes.Http.Request.HttpRequest,
            ) {
                write("presign#L(input) { expiresAfter = duration }", presignableOp.shape.id.name)
            }
    }

    private fun renderFullPresignFn(
        ctx: CodegenContext,
        writer: KotlinWriter,
        service: ServiceShape,
        serviceSymbol: Symbol,
        sigv4ServiceName: String,
        presignableOp: PresignableOperation,
        requestSymbol: Symbol,
        serializerSymbol: Symbol,
        contextMap: Map<SectionKey<*>, Any>,
    ) = writer.apply {
        dokka {
            write("Presign a [#T] using the configuration of this [#T].", requestSymbol, serviceSymbol)
            write("@param input The [#T] to presign", requestSymbol)
            write(
                "@param signer The specific implementation of AWS signer to use. Defaults to #T.",
                RuntimeTypes.Auth.Signing.AwsSigningStandard.DefaultAwsSigner,
            )
            write("@param configBlock A builder block for setting custom signing parameters. At a minimum the")
            write("[expiresAfter] field must be set.")
            write(
                "@return An [#T] which can be invoked within the specified time window",
                RuntimeTypes.Http.Request.HttpRequest,
            )
        }

        openBlock("public suspend fun #T.presign#L(", serviceSymbol, presignableOp.shape.id.name)
        write("input: #T,", requestSymbol)
        write(
            "signer: #T = #T,",
            RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigner,
            RuntimeTypes.Auth.Signing.AwsSigningStandard.DefaultAwsSigner,
        )
        write(
            "configBlock: #T.Builder.() -> Unit,",
            RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningConfig,
        )
        closeAndOpenBlock("): #T {", RuntimeTypes.Http.Request.HttpRequest)

        withBlock("val ctx = #T().apply {", "}", RuntimeTypes.Core.ExecutionContext) {
            write("set(#T.OperationName, #S)", RuntimeTypes.SmithyClient.SdkClientOption, presignableOp.shape.id.name)
            write("set(#T.OperationInput, input)", RuntimeTypes.HttpClient.Operation.HttpOperationContext)
        }
        write("val unsignedRequest = #T().serialize(ctx, input)", serializerSymbol)
        if (presignableOp.liftBodyToQueryString) {
            write("unsignedRequest.method = #T.GET", RuntimeTypes.Http.HttpMethod)
            withBlock(
                "unsignedRequest.body.#T()?.#T()?.let {",
                "}",
                RuntimeTypes.Http.toByteStream,
                RuntimeTypes.Core.Content.decodeToString,
            ) {
                write("val bodyParams = #T.parseEncoded(it)", RuntimeTypes.Core.Net.Url.QueryParameters)
                write("unsignedRequest.url.parameters.addAll(bodyParams)")
            }
            write("")
        }

        write("val endpointResolver = #T(config)", EndpointResolverAdapterGenerator.getSymbol(ctx.settings))
        write("")

        declareSection(UnsignedRequestCustomizationSection, contextMap)

        withBlock(
            "return #T(unsignedRequest, ctx, config.credentialsProvider, endpointResolver, signer) {",
            "}",
            RuntimeTypes.Auth.Signing.AwsSigningCommon.presignRequest,
        ) {
            // The signing context may have already been overridden by endpoint attributes. If not, then set
            // service/region here.
            write("if (service == null) service = #S", sigv4ServiceName)
            write("if (region == null) region = config.region")

            if (!useDoubleUriEncode(service)) write("useDoubleUriEncode = false")
            if (!normalizeUriPath(service)) write("normalizeUriPath = false")
            if (presignableOp.signBody) {
                write(
                    "hashSpecification = #T.CalculateFromPayload",
                    RuntimeTypes.Auth.Signing.AwsSigningCommon.HashSpecification,
                )
            }

            declareSection(SigningConfigCustomizationSection)

            write("configBlock()")
        }
        closeBlock("}")
    }

    /**
     * Determine `useDoubleUriEncode` setting based on service.
     * From [SigV4 documentation](https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html):
     * > Each path segment must be URI-encoded twice (except for Amazon S3 which only gets URI-encoded once).
     */
    private fun useDoubleUriEncode(service: ServiceShape) = service.sdkId != "S3"

    /**
     * Determine `normalizeUriPath` setting based on service.
     * From [S3 signing documentation](https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html#canonical-request):
     * > You do not normalize URI paths for requests to Amazon S3. For example, you may have a bucket with an object
     * > named "my-object//example//photo.user". Normalizing the path changes the object name in the request to
     * > "my-object/example/photo.user". This is an incorrect path for that object.
     */
    private fun normalizeUriPath(service: ServiceShape) = service.sdkId != "S3"
}
