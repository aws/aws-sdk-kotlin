/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.endpoints

import aws.sdk.kotlin.runtime.config.profile.*
import aws.sdk.kotlin.runtime.config.profile.FileType
import aws.sdk.kotlin.runtime.config.profile.parse
import aws.sdk.kotlin.runtime.config.profile.toSharedConfig
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveEndpointDiscoveryTest {
    @Test
    fun testPrecedenceSysProps() = assertEpDiscovery(
        sysProps = mapOf("aws.endpointDiscoveryEnabled" to "true"),
        env = mapOf("AWS_ENABLE_ENDPOINT_DISCOVERY" to "false"),
        config = """
            [${Literals.DEFAULT_PROFILE}]
            endpoint_discovery_enabled = false
        """.trimIndent(),
        serviceRequiresEpDiscovery = false,
        expected = true,
    )

    @Test
    fun testPrecedenceEnvVars() = assertEpDiscovery(
        env = mapOf("AWS_ENABLE_ENDPOINT_DISCOVERY" to "true"),
        config = """
            [${Literals.DEFAULT_PROFILE}]
            endpoint_discovery_enabled = false
        """.trimIndent(),
        serviceRequiresEpDiscovery = false,
        expected = true,
    )

    @Test
    fun testPrecedenceConfig() = assertEpDiscovery(
        config = """
            [${Literals.DEFAULT_PROFILE}]
            endpoint_discovery_enabled = true
        """.trimIndent(),
        serviceRequiresEpDiscovery = false,
        expected = true,
    )

    @Test
    fun testPrecedenceDefault() = assertEpDiscovery(
        serviceRequiresEpDiscovery = true,
        expected = true,
    )
}

fun assertEpDiscovery(
    sysProps: Map<String, String> = mapOf(),
    env: Map<String, String> = mapOf(),
    config: String = "",
    serviceRequiresEpDiscovery: Boolean,
    expected: Boolean,
) = runTest {
    val provider = TestPlatformProvider.of(env = env, props = sysProps)
    val source = AwsConfigurationSource(Literals.DEFAULT_PROFILE, "", "")

    val profile = asyncLazy {
        parse(Logger.None, FileType.CONFIGURATION, config)
            .toSharedConfig(source)
            .activeProfile
    }

    val actual = resolveEndpointDiscoveryEnabled(provider, profile, serviceRequiresEpDiscovery)
    assertEquals(expected, actual)
}
