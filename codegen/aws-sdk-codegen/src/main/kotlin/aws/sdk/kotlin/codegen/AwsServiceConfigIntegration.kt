/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.asNullable
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.model.nullable
import software.amazon.smithy.kotlin.codegen.rendering.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.kotlin.codegen.rendering.util.RuntimeConfigProperty
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.traits.HttpBearerAuthTrait

class AwsServiceConfigIntegration : KotlinIntegration {
    companion object {
        // Override the region property registered by AWS protocol support
        val RegionProp: ConfigProperty = ConfigProperty {
            name = "region"
            symbol = KotlinTypes.String.toBuilder().nullable().build()
            baseClass = AwsRuntimeTypes.Config.AwsSdkClientConfig
            useNestedBuilderBaseClass()
            documentation = """
                The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
                [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more information.
                When specified, this static region configuration takes precedence over other region resolution methods.
                
                The region resolution order is:
                1. Static region (if specified)
                2. Custom region provider (if configured)
                3. Default region provider chain              
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { prop, writer ->
                    writer.write(
                        "override val #1L: #2T? = builder.#1L ?: #3T { builder.regionProvider?.getRegion() ?: #4T() }",
                        prop.propertyName,
                        prop.symbol,
                        RuntimeTypes.KotlinxCoroutines.runBlocking,
                        AwsRuntimeTypes.Config.Region.resolveRegion,
                    )
                },
            )
            order = -100
        }

        val RegionProviderProp: ConfigProperty = ConfigProperty {
            name = "regionProvider"
            symbol = RuntimeTypes.SmithyClient.Region.RegionProvider
            baseClass = AwsRuntimeTypes.Config.AwsSdkClientConfig
            useNestedBuilderBaseClass()
            documentation = """
                An optional region provider that determines the AWS region for client operations. When specified, this provider 
                takes precedence over the default region provider chain, unless a static region is explicitly configured. 
                
                The region resolution order is:
                1. Static region (if specified)
                2. Custom region provider (if configured)
                3. Default region provider chain
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { prop, writer ->
                    writer.write(
                        "override val #1L: #2T = builder.#1L ?: #3T()",
                        prop.propertyName,
                        prop.symbol,
                        AwsRuntimeTypes.Config.Region.DefaultRegionProviderChain,
                    )
                },
            )
            order = -99 // After RegionProp
        }

        val UserAgentAppId: ConfigProperty = ConfigProperty {
            name = "applicationId"
            symbol = KotlinTypes.String.asNullable()
            baseClass = AwsRuntimeTypes.Config.AwsSdkClientConfig
            useNestedBuilderBaseClass()
            documentation = """
                 An optional application specific identifier.
                 When set it will be appended to the User-Agent header of every request in the form of: `app/{applicationId}`.
                 When not explicitly set, the value will be loaded from the following locations:
                 
                 - JVM System Property: `aws.userAgentAppId`
                 - Environment variable: `AWS_SDK_UA_APP_ID`
                 - Shared configuration profile attribute: `sdk_ua_app_id`
                 
                 See [shared configuration settings](https://docs.aws.amazon.com/sdkref/latest/guide/settings-reference.html)
                 reference for more information on environment variables and shared config settings.
            """.trimIndent()
            order = 100
        }

        // override the credentials provider prop registered by the Sigv4AuthSchemeIntegration, updates the
        // documentation and sets a default value for AWS SDK to the default chain.
        val CredentialsProviderProp: ConfigProperty = ConfigProperty {
            symbol = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider
            baseClass = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProviderConfig
            useNestedBuilderBaseClass()
            documentation = """
                The AWS credentials provider to use for authenticating requests. If not provided a
                [${AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider}] instance will be used.
                NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
                client will not close it when the client is closed.
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { prop, writer ->
                    writer.write(
                        "override val #1L: #2T = builder.#1L ?: #3T(httpClient = httpClient, region = region).#4T()",
                        prop.propertyName,
                        prop.symbol,
                        AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider,
                        AwsRuntimeTypes.Config.Credentials.manage,
                    )
                },
            )
        }

        val AwsRetryPolicy = RuntimeConfigProperty
            .RetryPolicy
            .toBuilder()
            .apply {
                propertyType = ConfigPropertyType.RequiredWithDefault("AwsRetryPolicy.Default")
                additionalImports = listOf(AwsRuntimeTypes.Http.Retries.AwsRetryPolicy)
            }
            .build()

        // override the bearer token provider registered by BearerTokenAuthSchemeIntegration, updates documentation
        // and configures the default to be the DefaultBearerTokenProviderChain
        val BearerTokenProviderProp = ConfigProperty {
            name = "bearerTokenProvider"
            symbol = RuntimeTypes.Auth.HttpAuth.BearerTokenProvider
            baseClass = RuntimeTypes.Auth.HttpAuth.BearerTokenProviderConfig
            useNestedBuilderBaseClass()
            documentation = """
                The token provider to use for authenticating requests when using [${RuntimeTypes.Auth.HttpAuth.BearerTokenAuthScheme.fullName}].
                If not provided a [${AwsRuntimeTypes.Config.Credentials.DefaultChainBearerTokenProvider}] instance will be used.
                NOTE: The caller is responsible for managing the lifetime of the provider when set. The SDK
                client will not close it when the client is closed.
                
            """.trimIndent()

            propertyType = ConfigPropertyType.Custom(
                render = { prop, writer ->
                    writer.write(
                        "override val #1L: #2T = builder.#1L ?: #3T(httpClient = httpClient).#4T()",
                        prop.propertyName,
                        prop.symbol,
                        AwsRuntimeTypes.Config.Credentials.DefaultChainBearerTokenProvider,
                        AwsRuntimeTypes.Config.Credentials.manage,
                    )
                },
            )
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(
            SectionWriterBinding(ServiceClientGenerator.Sections.CompanionObject, ServiceClientCompanionObjectWriter()),
            SectionWriterBinding(ServiceClientGenerator.Sections.CompanionObject.SuperTypes) { writer, _ ->
                writer.write(
                    "#T<Config, Config.Builder, #T, Builder>()",
                    AwsRuntimeTypes.Config.AbstractAwsSdkClientFactory,
                    writer.getContextValue(ServiceClientGenerator.Sections.CompanionObject.ServiceSymbol),
                )
            },
            SectionWriterBinding(
                HttpProtocolClientGenerator.MergeServiceDefaults,
                AppendingSectionWriter { writer ->
                    writer.write("ctx.#T(#T.ApplicationId, config.#L)", RuntimeTypes.Core.Collections.putIfAbsentNotNull, AwsRuntimeTypes.Config.AwsSdkClientOption, UserAgentAppId.propertyName)
                },
            ),
        )

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = buildList {
        add(RegionProp)
        add(RegionProviderProp)
        if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.settings.getService(ctx.model))) {
            add(CredentialsProviderProp)
        }

        val serviceIndex = ServiceIndex.of(ctx.model)
        val hasBearerTokenAuth = serviceIndex
            .getAuthSchemes(ctx.settings.service)
            .containsKey(HttpBearerAuthTrait.ID)
        if (hasBearerTokenAuth) {
            add(BearerTokenProviderProp)
        }

        add(AwsRetryPolicy)
        add(UserAgentAppId)
    }
}
