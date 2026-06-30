/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.*
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImdsRegionProviderTest {

    @Test
    fun testImdsDisabled() = runTest {
        val platform = TestPlatformProvider.of(
            env = mapOf(AwsSdkSetting.AwsEc2MetadataDisabled.envVar to "true"),
        )

        val provider = ImdsRegionProvider(platformProvider = platform)
        assertNull(provider.getRegion())
    }

    @Test
    fun testResolveRegion() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/placement/region", "TOKEN_A"),
                imdsResponse("us-east-2"),
            )
        }

        val testClock = ManualClock()

        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsRegionProvider(client = lazyOf(client), platformProvider = TestPlatformProvider.of())
        assertEquals("us-east-2", provider.getRegion())
        connection.assertRequests()

        // test that it's cached, test connection would fail if it tries again
        assertEquals("us-east-2", provider.getRegion())
    }
}
