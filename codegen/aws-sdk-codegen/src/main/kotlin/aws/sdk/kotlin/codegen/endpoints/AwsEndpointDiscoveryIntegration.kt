/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.ServiceClientCompanionObjectWriter
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinDelegator
import aws.smithy.kotlin.codegen.core.getContextValue
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.model.asNullable
import aws.smithy.kotlin.codegen.rendering.endpoints.discovery.DefaultEndpointDiscovererGenerator
import aws.smithy.kotlin.codegen.rendering.endpoints.discovery.EndpointDiscovererInterfaceGenerator
import aws.smithy.kotlin.codegen.rendering.endpoints.discovery.EndpointDiscoveryIntegration
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import aws.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.model.Model

class AwsEndpointDiscoveryIntegration : KotlinIntegration {
    override val order: Byte = (EndpointDiscoveryIntegration.ORDER + 1).toByte() // after EndpointDiscoveryIntegration

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> {
        val endpointDiscoveryOptional = EndpointDiscoveryIntegration.isOptionalFor(ctx)
        val interfaceSymbol = EndpointDiscovererInterfaceGenerator.symbolFor(ctx.settings)
        return listOf(
            ConfigProperty {
                name = EndpointDiscoveryIntegration.CLIENT_CONFIG_NAME
                symbol = interfaceSymbol.asNullable()

                if (endpointDiscoveryOptional) {
                    documentation = """
                        The endpoint discoverer for this client, if applicable. By default, no endpoint discovery is
                        provided. To use endpoint discovery, set this to a valid [${interfaceSymbol.name}] instance.
                    """.trimIndent()
                    propertyType = ConfigPropertyType.SymbolDefault
                } else {
                    val defaultImplSymbol = DefaultEndpointDiscovererGenerator.symbolFor(ctx.settings)

                    documentation = """
                        The endpoint discoverer for this client, [${defaultImplSymbol.name}] by default.
                    """.trimIndent()
                    propertyType = ConfigPropertyType.Custom(
                        render = { prop, writer ->
                            writer.write(
                                "#1L val #2L: #3T = builder.#2L ?: #4T()",
                                ctx.settings.api.visibility,
                                prop.propertyName,
                                prop.symbol,
                                defaultImplSymbol,
                            )
                        },
                    )
                }
            },
        )
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = EndpointDiscoveryIntegration.isEnabledFor(model, settings)

    private val resolveEndpointDiscoverer = AppendingSectionWriter { writer ->
        val ctx = writer.getContextValue(CodegenContext.Key)
        val endpointDiscoveryOptional = EndpointDiscoveryIntegration.isOptionalFor(ctx)

        writer.write(
            "val epDiscoveryEnabled = #T(profile = activeProfile, serviceRequiresEpDiscovery = #L)",
            AwsRuntimeTypes.Config.Endpoints.resolveEndpointDiscoveryEnabled,
            !endpointDiscoveryOptional,
        )

        writer.write(
            "builder.config.#1L = builder.config.#1L ?: if (epDiscoveryEnabled) #2T() else null",
            EndpointDiscoveryIntegration.CLIENT_CONFIG_NAME,
            DefaultEndpointDiscovererGenerator.symbolFor(ctx.settings),
        )
    }

    override val sectionWriters = listOf(
        SectionWriterBinding(ServiceClientCompanionObjectWriter.FinalizeEnvironmentalConfig, resolveEndpointDiscoverer),
    )

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        // EndpointDiscoveryIntegration already renders the default endpoint discoverer for services that _require_ EP
        // discovery. So we only need to render it for services which _do not require_ EP discovery in order to support
        // enabling discovery via environmental config.
        if (EndpointDiscoveryIntegration.isOptionalFor(ctx)) {
            DefaultEndpointDiscovererGenerator(ctx, delegator).render()
        }
    }
}
