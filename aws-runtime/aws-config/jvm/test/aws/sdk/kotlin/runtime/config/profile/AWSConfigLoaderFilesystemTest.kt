/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.config.utils.mockPlatform
import aws.smithy.kotlin.runtime.testing.withTempDir
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that exercise logic associated with the filesystem
 */
class AWSConfigLoaderFilesystemTest {

    @Test
    fun itLoadsConfigFileFromFilesystem() = runTest {
        withTempDir { dir ->
            val configFile = Path(dir, "config")
            val credentialsFile = Path(dir, "credentials")

            SystemFileSystem.sink(configFile).buffered().use { it.writeString("[profile foo]\nname = value") }

            val testPlatform = mockPlatform(
                pathSegment = PlatformProvider.System.filePathSeparator, // Use actual value from Platform in mock
                awsProfileEnv = "foo",
                homeEnv = "/home/user",
                awsConfigFileEnv = configFile.toString(),
                awsSharedCredentialsFileEnv = credentialsFile.toString(),
                os = PlatformProvider.System.osInfo(), // Actual value
            )

            val actual = loadAwsSharedConfig(testPlatform).activeProfile

            assertEquals("foo", actual.name)
            assertEquals("value", actual.getOrNull("name"))
        }
    }

    @Test
    fun itLoadsConfigAndCredsFileFromFilesystem() = runTest {
        withTempDir { dir ->
            val configFile = Path(dir, "config")
            val credentialsFile = Path(dir, "credentials")

            SystemFileSystem.sink(configFile).buffered().use { it.writeString("[profile default]\nname = value\n[default]\nname2 = value2\n[profile default]\nname3 = value3") }
            SystemFileSystem.sink(credentialsFile).buffered().use { it.writeString("[default]\nsecret=foo") }

            val testPlatform = mockPlatform(
                pathSegment = PlatformProvider.System.filePathSeparator, // Use actual value from Platform in mock
                homeEnv = "/home/user",
                awsConfigFileEnv = configFile.toString(),
                awsSharedCredentialsFileEnv = credentialsFile.toString(),
                os = PlatformProvider.System.osInfo(), // Actual value
            )

            val actual = loadAwsSharedConfig(testPlatform).activeProfile

            assertEquals("default", actual.name)
            assertEquals("value", actual.getOrNull("name"))
            assertEquals("value3", actual.getOrNull("name3"))
            assertEquals("foo", actual.getOrNull("secret"))
        }
    }
}
