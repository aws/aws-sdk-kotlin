/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.endpoints

import aws.sdk.kotlin.runtime.config.profile.AwsConfigurationSource
import aws.sdk.kotlin.runtime.config.profile.FileType
import aws.sdk.kotlin.runtime.config.profile.parse
import aws.sdk.kotlin.runtime.config.profile.toSharedConfig
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ResolveEndpointUrlTest {
    @Test
    fun testEndpointUrlDisabledSys() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
            "AWS_ENDPOINT_URL_ELASTIC_BEANSTALK" to "https://env-elastic-beanstalk.dev",
        ),
        sysProps = mapOf(
            "aws.ignoreConfiguredEndpointUrls" to "true",
            "aws.endpointUrl" to "https://sys-global.dev",
            "aws.endpointUrlElasticBeanstalk" to "https://sys-elastic-beanstalk.dev",
        ),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = null,
    )

    @Test
    fun testEndpointUrlDisabledEnv() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_IGNORE_CONFIGURED_ENDPOINT_URLS" to "true",
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
            "AWS_ENDPOINT_URL_ELASTIC_BEANSTALK" to "https://env-elastic-beanstalk.dev",
        ),
        sysProps = mapOf(
            "aws.endpointUrl" to "https://sys-global.dev",
            "aws.endpointUrlElasticBeanstalk" to "https://sys-elastic-beanstalk.dev",
        ),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = null,
    )

    @Test
    fun testEndpointUrlDisabledConfig() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
            "AWS_ENDPOINT_URL_ELASTIC_BEANSTALK" to "https://env-elastic-beanstalk.dev",
        ),
        sysProps = mapOf(
            "aws.endpointUrl" to "https://sys-global.dev",
            "aws.endpointUrlElasticBeanstalk" to "https://sys-elastic-beanstalk.dev",
        ),
        config = """
            [profile default]
            ignore_configured_endpoint_urls = true
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = null,
    )

    @Test
    fun testServiceEndpointUrlSys() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
            "AWS_ENDPOINT_URL_ELASTIC_BEANSTALK" to "https://env-elastic-beanstalk.dev",
        ),
        sysProps = mapOf(
            "aws.endpointUrl" to "https://sys-global.dev",
            "aws.endpointUrlElasticBeanstalk" to "https://sys-elastic-beanstalk.dev",
        ),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = "https://sys-elastic-beanstalk.dev",
    )

    @Test
    fun testServiceEndpointUrlEnv() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
            "AWS_ENDPOINT_URL_ELASTIC_BEANSTALK" to "https://env-elastic-beanstalk.dev",
        ),
        sysProps = mapOf(
            "aws.endpointUrl" to "https://sys-global.dev",
        ),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = "https://env-elastic-beanstalk.dev",
    )

    @Test
    fun testGlobalEndpointUrlSys() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
        ),
        sysProps = mapOf(
            "aws.endpointUrl" to "https://sys-global.dev",
        ),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = "https://sys-global.dev",
    )

    @Test
    fun testGlobalEndpointUrlEnv() = testResolveEndpointUrl(
        env = mapOf(
            "AWS_ENDPOINT_URL" to "https://env-global.dev",
        ),
        sysProps = mapOf(),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = "https://env-global.dev",
    )

    @Test
    fun testServiceEndpointUrlConfig() = testResolveEndpointUrl(
        env = mapOf(),
        sysProps = mapOf(),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-elastic-beanstalk.dev
        """.trimIndent(),
        expect = "https://config-elastic-beanstalk.dev",
    )

    @Test
    fun testGlobalEndpointUrlConfig() = testResolveEndpointUrl(
        env = mapOf(),
        sysProps = mapOf(),
        config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = foo

            [services foo]
        """.trimIndent(),
        expect = "https://config-global.dev",
    )

    @Test
    fun testNoConfig() = testResolveEndpointUrl(
        env = mapOf(),
        sysProps = mapOf(),
        config = "[default]",
        expect = null,
    )

    @Test
    fun testIgnoreGlobalEndpointUrlInServiceSection() = testResolveEndpointUrl(
        env = mapOf(),
        sysProps = mapOf(),
        config = """
                [profile default]
                endpoint_url = https://config-global.dev
                services = foo

                [services foo]
                endpoint_url = https://ignore-config-global.dev
        """.trimIndent(),
        expect = "https://config-global.dev",
    )

    @Test
    fun testHandleNonexistentServiceSection() = runTest {
        val config = """
            [profile default]
            endpoint_url = https://config-global.dev
            services = nonexistent

            [services foo]
            elastic_beanstalk =
              endpoint_url = https://config-global.dev
        """.trimIndent()

        val ex = assertFails { testResolveEndpointUrl(env = mapOf(), sysProps = mapOf(), config = config, expect = "nothing") }
        assertEquals("shared config points to nonexistent services section 'nonexistent'", ex.message)
    }
}

fun testResolveEndpointUrl(
    env: Map<String, String>,
    sysProps: Map<String, String>,
    config: String,
    expect: String?,
    configProfile: String = "default",
    sysPropSuffix: String = "ElasticBeanstalk",
    envSuffix: String = "ELASTIC_BEANSTALK",
    sharedConfigKey: String = "elastic_beanstalk",
) = runTest {
    val provider = TestPlatformProvider.of(env = env, props = sysProps)
    val source = AwsConfigurationSource(configProfile, "", "")
    val sharedConfig = asyncLazy { parse(Logger.None, FileType.CONFIGURATION, config).toSharedConfig(source) }

    val actualUrl = resolveEndpointUrl(sharedConfig, sysPropSuffix, envSuffix, sharedConfigKey, provider)
    assertEquals(expect?.let(Url::parse), actualUrl)
}
