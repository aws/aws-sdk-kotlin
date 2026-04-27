/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.ServiceClientCompanionObjectWriter
import aws.sdk.kotlin.codegen.endpoints.AwsBuiltins
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.getEndpointRules
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import aws.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Registers support for the `AWS::Auth::AccountId` endpoint builtin
 */
class AccountIdEndpointBuiltinCustomization : KotlinIntegration {
    companion object {

        val AccountIdEndpointModeProp = ConfigProperty {
            name = "accountIdEndpointMode"
            symbol = AwsRuntimeTypes.Config.Endpoints.AccountIdEndpointMode
            documentation = """
                Control the way account ID is bound to the endpoint resolver parameters. 
                Defaults to [AccountIdEndpointMode.PREFERRED].
            """.trimIndent()
            propertyType = ConfigPropertyType.RequiredWithDefault("AccountIdEndpointMode.PREFERRED")
        }
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val rules = model.expectShape<ServiceShape>(settings.service).getEndpointRules()
        return rules?.parameters?.find { it.isBuiltIn && it.builtIn.get() == AwsBuiltins.ACCOUNT_ID } != null
    }

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                ServiceClientCompanionObjectWriter.FinalizeEnvironmentalConfig,
                resolveAccountIdEndpointModeSectionWriter,
            ),
        )

    private val resolveAccountIdEndpointModeSectionWriter = AppendingSectionWriter { writer ->
        writer.write(
            "builder.config.#1L = builder.config.#1L ?: #2T(profile = activeProfile)",
            AccountIdEndpointModeProp.propertyName,
            AwsRuntimeTypes.Config.Endpoints.resolveAccountIdEndpointMode,
        )
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(AccountIdEndpointModeProp)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + listOf(emitAccountIdEndpointModeMiddleware)
}

private val emitAccountIdEndpointModeMiddleware = object : ProtocolMiddleware {
    override val name: String = "EmitAccountIdEndpointModeMiddleware"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write(
            "op.context.emitBusinessMetric(config.accountIdEndpointMode.#T())",
            AwsRuntimeTypes.Config.Endpoints.toBusinessMetric,
        )
    }
}
