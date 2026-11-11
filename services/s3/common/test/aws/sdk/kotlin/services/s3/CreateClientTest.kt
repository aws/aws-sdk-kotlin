/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.s3

import aws.smithy.kotlin.runtime.io.use
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Validate the way service clients can be constructed.
 *
 * These are written against S3 but apply generically to any client.
 */
class CreateClientTest {
    @Test
    fun testMissingRegion() {
        // Should _not_ throw an exception since region is optional
        S3Client { }.use { }
    }

    @Test
    fun testDefaults() {
        S3Client { region = "us-east-2" }.use { }
    }

    @Test
    fun testFromEnvironmentWithOverrides() = runTest {
        S3Client.fromEnvironment { region = "us-east-2" }.use { }
    }
}
