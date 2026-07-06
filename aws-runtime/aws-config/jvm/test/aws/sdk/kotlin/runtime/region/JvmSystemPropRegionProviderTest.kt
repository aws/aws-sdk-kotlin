/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmSystemPropRegionProviderTest {

    @Test
    fun testGetRegion() = runTest {
        val provider = JvmSystemPropRegionProvider(TestPlatformProvider.of())

        assertNull(provider.getRegion())

        val provider2 = JvmSystemPropRegionProvider(
            TestPlatformProvider.of(
                props = mapOf("aws.region" to "us-east-1"),
            ),
        )
        assertEquals("us-east-1", provider2.getRegion())
    }
}
