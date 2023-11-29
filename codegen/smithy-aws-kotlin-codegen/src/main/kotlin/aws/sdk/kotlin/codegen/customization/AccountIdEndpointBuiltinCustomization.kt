/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.endpoints.AwsBuiltins
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getEndpointRules
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.model.Model
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
        get() = listOf(SectionWriterBinding(ServiceClientGenerator.Sections.FinalizeConfig, resolveAccountIdEndpointModeSectionWriter))

    private val resolveAccountIdEndpointModeSectionWriter = AppendingSectionWriter { writer ->
        writer.write(
            "builder.config.#L = #T(profile = activeProfile)",
            AccountIdEndpointModeProp.propertyName,
            AwsRuntimeTypes.Config.Endpoints.resolveAccountIdEndpointMode,
        )
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(AccountIdEndpointModeProp)
}
