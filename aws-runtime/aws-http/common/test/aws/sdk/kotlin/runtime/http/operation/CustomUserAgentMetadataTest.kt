/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.operation

import aws.sdk.kotlin.runtime.http.ApiMetadata
import aws.sdk.kotlin.runtime.http.loadAwsUserAgentMetadataFromEnvironment
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomUserAgentMetadataTest {
    @Test
    fun testCustomMetadata() {
        val provider = TestPlatformProvider.of()
        val metadata = loadAwsUserAgentMetadataFromEnvironment(provider, ApiMetadata("Test Service", "1.2.3"))
        val customMetadata = CustomUserAgentMetadata()

        customMetadata.add("foo", "bar")
        customMetadata.add("truthy", "true")
        customMetadata.add("falsey", "false")

        val configMetadata = ConfigMetadata("retry-mode", "standard")
        customMetadata.add(configMetadata)

        customMetadata.add(FeatureMetadata("s3-transfer", "1.2.3"))
        customMetadata.add(FeatureMetadata("waiter"))

        val actual = metadata.copy(customMetadata = customMetadata).xAmzUserAgent

        listOf(
            "md/foo#bar",
            "md/truthy",
            "md/falsey#false",
            "cfg/retry-mode#standard",
            "ft/s3-transfer#1.2.3",
            "ft/waiter",
        ).forEach { partial ->
            actual.shouldContain(partial)
        }
    }

    @Test
    fun testFromEnvironment() {
        val props = mapOf(
            "irrelevantProp" to "shouldBeIgnored",
            "aws.customMetadata" to "shouldBeIgnored",
            "aws.customMetadata.foo" to "bar",
            "aws.customMetadata.baz" to "qux",
            "aws.customMetadata.priority" to "props",
        )
        val envVars = mapOf(
            "IRRELEVANT_PROP" to "shouldBeIgnored",
            "AWS_CUSTOM_METADATA" to "shouldBeIgnored",
            "AWS_CUSTOM_METADATA_oof" to "rab",
            "AWS_CUSTOM_METADATA_zab" to "xuq",
            "AWS_CUSTOM_METADATA_priority" to "envVars",
        )
        val provider = TestPlatformProvider.of(env = envVars, props = props)
        val metadata = CustomUserAgentMetadata.fromEnvironment(provider)

        val expected = mapOf(
            "foo" to "bar",
            "baz" to "qux",
            "oof" to "rab",
            "zab" to "xuq",
            "priority" to "props", // System properties take precedence over env vars
        )
        assertEquals(expected, metadata.extras)
    }
}
