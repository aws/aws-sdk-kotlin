/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionId
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator
import software.amazon.smithy.kotlin.codegen.utils.toPascalCase

/**
 * Overrides the service client companion object for how a client is constructed.
 *
 * Includes the ability to extend the config finalizer, which by default handles resolution of endpoint url config.
 */
class ServiceClientCompanionObjectWriter : AppendingSectionWriter {
    /**
     * The [SectionId] used for rendering the `finalizeEnvironmentalConfig` method body.
     */
    object FinalizeEnvironmentalConfig : SectionId

    override fun append(writer: KotlinWriter) {
        val funName = "finalizeEnvironmentalConfig"
        writer.write("")
        writer.withBlock(
            "override suspend fun #1L(builder: Builder, sharedConfig: #2T<#3T>, activeProfile: #2T<#4T>) {",
            "}",
            funName,
            RuntimeTypes.Core.Utils.LazyAsyncValue,
            AwsRuntimeTypes.Config.Profile.AwsSharedConfig,
            AwsRuntimeTypes.Config.Profile.AwsProfile,
        ) {
            declareSection(FinalizeEnvironmentalConfig) {
                write("super.#L(builder, sharedConfig, activeProfile)", funName)
                writeResolveEndpointUrl()
            }
        }
    }

    private fun KotlinWriter.writeResolveEndpointUrl() {
        withBlock(
            "builder.config.endpointUrl = builder.config.endpointUrl ?: #T(",
            ")",
            AwsRuntimeTypes.Config.Endpoints.resolveEndpointUrl,
        ) {
            val sdkId = getContextValue(ServiceClientGenerator.Sections.CompanionObject.SdkId)
            val names = sdkId.toEndpointUrlConfigNames()

            write("sharedConfig,")
            write("#S,", names.sysPropSuffix)
            write("#S,", names.envSuffix)
            write("#S,", names.sharedConfigKey)
        }
    }
}

internal data class EndpointUrlConfigNames(
    val sysPropSuffix: String,
    val envSuffix: String,
    val sharedConfigKey: String,
)

internal fun String.toEndpointUrlConfigNames(): EndpointUrlConfigNames = EndpointUrlConfigNames(
    withTransform(JvmSystemPropertySuffix),
    withTransform(SdkIdTransformers.UpperSnakeCase),
    withTransform(SdkIdTransformers.LowerSnakeCase),
)

// JVM system property names follow the pattern "aws.endpointUrl${BaseClientName}"
// where BaseClientName is the PascalCased sdk ID with any forbidden suffixes dropped - this is the same as what we use
// for our client names
// e.g. sdkId "Elasticsearch Service" -> client name "ElasticsearchClient", prop "aws.endpointUrlElasticsearch"
private object JvmSystemPropertySuffix : StringTransformer {
    override fun transform(id: String): String =
        id.toPascalCase().removeSuffix("Service")
}
