/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AwsClientConfigLoaderTest {
    @Test
    fun testExplicit() = runTest {
        val provider = TestPlatformProvider()
        val actual = loadAwsClientConfig(provider) {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "AKID"
                secretAccessKey = "secret"
            }
        }
        assertEquals("us-east-2", actual.region)
        assertEquals("AKID", actual.credentialsProvider.getCredentials().accessKeyId)
    }
}
