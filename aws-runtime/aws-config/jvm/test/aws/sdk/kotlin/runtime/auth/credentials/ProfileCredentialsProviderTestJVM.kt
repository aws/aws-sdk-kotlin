/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.withBusinessMetrics
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.mockk.coEvery
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileCredentialsProviderTestJVM {
    @Test
    fun processBusinessMetrics() = runTest {
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to """
                [default]
                credential_process = awscreds-custom
                """.trimIndent(),
                "awscreds-custom" to "some-process",
            ),
        )
        val testEngine = TestConnection()
        val provider = ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
        )

        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(
            Pair(
                0,
                """
            {
                "Version": 1,
                "AccessKeyId": "AKID-Default",
                "SecretAccessKey": "Default-Secret",
                "SessionToken": "SessionToken",
                "Expiration" : "2019-05-29T00:21:43Z"
            }
                """.trimIndent(),
            ),
        )

        val actual = provider.resolve()
        val expected = credentials(
            "AKID-Default",
            "Default-Secret",
            "SessionToken",
            Instant.fromIso8601("2019-05-29T00:21:43Z"),
            "Process",
        ).withBusinessMetrics(
            setOf(
                AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_PROCESS,
                AwsBusinessMetric.Credentials.CREDENTIALS_PROCESS,
            ),
        )

        assertEquals(expected, actual)
    }
}
