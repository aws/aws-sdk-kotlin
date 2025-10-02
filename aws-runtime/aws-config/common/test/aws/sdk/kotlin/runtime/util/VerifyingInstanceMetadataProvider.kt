/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.util

import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.asserter

class VerifyingInstanceMetadataProvider(expectations: List<Pair<String, () -> String>>) : InstanceMetadataProvider {
    private val expectations = expectations.toMutableList()

    override fun close() = Unit

    override suspend fun get(path: String): String {
        val trimmedPath = path.trimEnd('/') // remove trailing slashes to simplify testing
        val next = assertNotNull(expectations.removeFirstOrNull(), "Call to \"$trimmedPath\" was unexpected!")
        val (expectedPath, result) = next
        assertEquals(trimmedPath, expectedPath, "Expected call to \"$expectedPath\" but got \"$trimmedPath\" instead!")
        return result()
    }

    fun verifyComplete() {
        asserter.assertTrue(
            lazyMessage = {
                buildString {
                    appendLine("Not all expectations were met! Remaining paths which were not called:")
                    expectations.map { it.first }.forEach { appendLine("- $it") }
                }
            },
            actual = expectations.isEmpty(),
        )
    }
}
