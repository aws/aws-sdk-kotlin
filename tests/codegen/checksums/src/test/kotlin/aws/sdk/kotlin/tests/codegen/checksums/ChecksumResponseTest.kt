/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.checksums

import aws.sdk.kotlin.tests.codegen.checksums.utils.runChecksumTest
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Test the SDK validates correct checksum values
 */
class SuccessfulChecksumResponseTest {
    @Test
    fun crc32() = runChecksumTest(
        responseChecksumHeader = "x-amz-checksum-crc32",
        responseChecksumValue = "i9aeUg==",
    )

    @Test
    fun crc32c() = runChecksumTest(
        responseChecksumHeader = "x-amz-checksum-crc32c",
        responseChecksumValue = "crUfeA==",
    )

    @Test
    fun sha1() = runChecksumTest(
        responseChecksumHeader = "x-amz-checksum-sha1",
        responseChecksumValue = "e1AsOh9IyGCa4hLN+2Od7jlnP14=",
    )

    @Test
    fun sha256() = runChecksumTest(
        responseChecksumHeader = "x-amz-checksum-sha256",
        responseChecksumValue = "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=",
    )
}

/**
 * Test the SDK throws exception on incorrect checksum values
 */
class IncorrectChecksumResponseTest {
    private val incorrectChecksumValue = "Kaboom!"

    @Test
    fun crc32() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumHeader = "x-amz-checksum-crc32",
                responseChecksumValue = incorrectChecksumValue,
            )
        }
    }

    @Test
    fun crc32c() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumHeader = "x-amz-checksum-crc32c",
                responseChecksumValue = incorrectChecksumValue,
            )
        }
    }

    @Test
    fun sha1() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumHeader = "x-amz-checksum-sha1",
                responseChecksumValue = incorrectChecksumValue,
            )
        }
    }

    @Test
    fun sha256() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumHeader = "x-amz-checksum-sha256",
                responseChecksumValue = incorrectChecksumValue,
            )
        }
    }
}
