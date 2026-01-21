/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.customization.AccountIdEndpointBuiltinCustomization
import aws.sdk.kotlin.codegen.customization.s3.express.S3ExpressIntegration
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.RuntimeTypes
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.defaultName
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointCustomization
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointParametersGenerator
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointResolverAdapterGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.rulesengine.language.EndpointRuleSet
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter
import aws.sdk.kotlin.codegen.customization.s3.ClientConfigIntegration as S3ClientConfigIntegration
import aws.sdk.kotlin.codegen.customization.s3control.ClientConfigIntegration as S3ControlClientConfigIntegration

/**
 * Integration that renders binding AWS builtin endpoint parameters
 */
class BindAwsEndpointBuiltins : KotlinIntegration {
    companion object {
        val UseFipsProp: ConfigProperty = ConfigProperty {
            name = "useFips"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to toggle whether to use [FIPS](https://aws.amazon.com/compliance/fips/) endpoints when making requests.
     `          Disabled by default.
            """.trimIndent()
            baseClass = AwsRuntimeTypes.Config.AwsSdkClientConfig
            useNestedBuilderBaseClass()
        }

        val UseDualStackProp: ConfigProperty = ConfigProperty {
            name = "useDualStack"
            useSymbolWithNullableBuilder(KotlinTypes.Boolean, "false")
            documentation = """
                Flag to toggle whether to use dual-stack endpoints when making requests.
                See [https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html] for more information.
     `          Disabled by default.
            """.trimIndent()
            baseClass = AwsRuntimeTypes.Config.AwsSdkClientConfig
            useNestedBuilderBaseClass()
        }
    }
    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(
        UseFipsProp,
        UseDualStackProp,
    )

    override fun customizeEndpointResolution(ctx: ProtocolGenerator.GenerationContext): EndpointCustomization = object : EndpointCustomization {
        override fun renderBindEndpointBuiltins(
            ctx: ProtocolGenerator.GenerationContext,
            rules: EndpointRuleSet,
            writer: KotlinWriter,
        ) {
            val builtins = rules.parameters?.toList()?.filter(Parameter::isBuiltIn) ?: return
            writer.write("#T(config, request)", bindAwsBuiltinsSymbol(ctx, builtins))
        }
    }
}

/**
 * Render binding of AWS SDK endpoint parameter builtins. In practice, all of these values are sourced from the client
 * config.
 */
fun renderBindAwsBuiltins(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter, builtinParams: List<Parameter>) {
    writer.withBlock(
        "private fun #T.Builder.bindAwsBuiltins(config: #T.Config, request: #T) {",
        "}",
        EndpointParametersGenerator.getSymbol(ctx.settings),
        ctx.symbolProvider.toSymbol(ctx.service),
        RuntimeTypes.HttpClient.Operation.ResolveEndpointRequest,
    ) {
        builtinParams.forEach {
            when (it.builtIn.get()) {
                AwsBuiltins.USE_FIPS -> renderBasicConfigBinding(writer, it, BindAwsEndpointBuiltins.UseFipsProp.propertyName)
                AwsBuiltins.USE_DUAL_STACK -> renderBasicConfigBinding(writer, it, BindAwsEndpointBuiltins.UseDualStackProp.propertyName)

                AwsBuiltins.S3_ACCELERATE -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.EnableAccelerateProp.propertyName)
                AwsBuiltins.S3_FORCE_PATH_STYLE -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.ForcePathStyleProp.propertyName)
                AwsBuiltins.S3_DISABLE_MRAP -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.DisableMrapProp.propertyName)
                AwsBuiltins.S3_DISABLE_EXPRESS_SESSION_AUTH -> renderBasicConfigBinding(writer, it, S3ExpressIntegration.DisableExpressSessionAuth.propertyName)
                AwsBuiltins.S3_USE_ARN_REGION -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.UseArnRegionProp.propertyName)
                AwsBuiltins.S3_CONTROL_USE_ARN_REGION -> renderBasicConfigBinding(writer, it, S3ControlClientConfigIntegration.UseArnRegionProp.propertyName)

                // as a newer SDK we do NOT support these values, they are always false
                AwsBuiltins.S3_USE_GLOBAL_ENDPOINT, AwsBuiltins.STS_USE_GLOBAL_ENDPOINT ->
                    writer.write("#L = false", it.defaultName())

                AwsBuiltins.ACCOUNT_ID ->
                    writer.write(
                        "#L = #T(config.#L, request.identity.attributes)",
                        it.defaultName(),
                        AwsRuntimeTypes.Config.Endpoints.resolveAccountId,
                        AccountIdEndpointBuiltinCustomization.AccountIdEndpointModeProp.propertyName,
                    )

                AwsBuiltins.ACCOUNT_ID_ENDPOINT_MODE -> {
                    writer.write(
                        "#L = config.#L.toString().lowercase()", // Spec specifies these enum values must be lowercase
                        it.defaultName(),
                        AccountIdEndpointBuiltinCustomization.AccountIdEndpointModeProp.propertyName,
                    )
                }
            }
        }
    }
}

fun bindAwsBuiltinsSymbol(ctx: ProtocolGenerator.GenerationContext, builtinParams: List<Parameter>): Symbol = buildSymbol {
    name = "bindAwsBuiltins"
    namespace = "${ctx.settings.pkg.name}.endpoints.internal"
    definitionFile = EndpointResolverAdapterGenerator.getSymbol(ctx.settings).definitionFile
    renderBy = { writer ->
        renderBindAwsBuiltins(ctx, writer, builtinParams)
    }
}

private fun renderBasicConfigBinding(writer: KotlinWriter, param: Parameter, configMember: String) {
    writer.write("#L = config.#L", param.defaultName(), configMember)
}
