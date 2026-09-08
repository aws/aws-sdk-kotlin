/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.util.TestFile
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FinalizeConfigTest {
    @Test
    fun testExplicitConfigTakesPrecedence() = runTest {
        val builder = S3Client.builder()
        builder.config.useArnRegion = false
        builder.config.disableMrap = true
        val platform = TestPlatformProvider.of(
            fs = mapOf(
                "/users/test/.aws/config" to TestFile("[default]\ns3_use_arn_region = true\ns3_disable_multiregion_access_points = false"),
            ),
        )
        val sharedConfig = asyncLazy { loadAwsSharedConfig(platform) }

        finalizeS3Config(builder, sharedConfig, platform)
        assertEquals(false, builder.config.useArnRegion)
        assertEquals(true, builder.config.disableMrap)
    }

    @Test
    fun testSystemProperties() = runTest {
        val builder = S3Client.builder()
        val platform = TestPlatformProvider.of(
            env = mapOf(
                S3Setting.UseArnRegion.envVar to "false",
                S3Setting.DisableMultiRegionAccessPoints.envVar to "true",
            ),
            props = mapOf(
                S3Setting.UseArnRegion.sysProp to "true",
                S3Setting.DisableMultiRegionAccessPoints.sysProp to "false",
            ),
            fs = mapOf(
                "/users/test/.aws/config" to TestFile("[default]\ns3_use_arn_region = false\ns3_disable_multiregion_access_points = true"),
            ),
        )
        val sharedConfig = asyncLazy { loadAwsSharedConfig(platform) }

        finalizeS3Config(builder, sharedConfig, platform)
        assertEquals(true, builder.config.useArnRegion)
        assertEquals(false, builder.config.disableMrap)
    }

    @Test
    fun testEnvironmentVariables() = runTest {
        val builder = S3Client.builder()
        val platform = TestPlatformProvider.of(
            env = mapOf(
                S3Setting.UseArnRegion.envVar to "false",
                S3Setting.DisableMultiRegionAccessPoints.envVar to "true",
            ),
            fs = mapOf(
                "/users/test/.aws/config" to TestFile("[default]\ns3_use_arn_region = true\ns3_disable_multiregion_access_points = false"),
            ),
        )
        val sharedConfig = asyncLazy { loadAwsSharedConfig(platform) }

        finalizeS3Config(builder, sharedConfig, platform)
        assertEquals(false, builder.config.useArnRegion)
        assertEquals(true, builder.config.disableMrap)
    }

    @Test
    fun testProfile() = runTest {
        val builder = S3Client.builder()
        val platform = TestPlatformProvider.of(
            fs = mapOf(
                "/users/test/.aws/config" to TestFile("[default]\ns3_use_arn_region = true\ns3_disable_multiregion_access_points = false"),
            ),
        )
        val sharedConfig = asyncLazy { loadAwsSharedConfig(platform) }

        finalizeS3Config(builder, sharedConfig, platform)
        assertEquals(true, builder.config.useArnRegion)
        assertEquals(false, builder.config.disableMrap)
    }

    @Test
    fun testConfigPropertiesPresent() {
        // regression test to verify that these config properties are generated and can be set
        // see https://github.com/awslabs/aws-sdk-kotlin/issues/1098
        val builder = S3Client.builder()
        builder.config.disableMrap = true
        builder.config.useArnRegion = true
        builder.config.forcePathStyle = true
        builder.config.enableAccelerate = true
        builder.config.useDualStack = true
        builder.config.useFips = true
    }
}
