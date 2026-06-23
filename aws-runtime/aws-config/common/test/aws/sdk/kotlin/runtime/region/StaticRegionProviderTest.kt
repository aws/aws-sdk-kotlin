/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticRegionProviderTest {
    @Test
    fun providesRegion() = runTest {
        val provider = StaticRegionProvider("us-west-2")
        assertEquals("us-west-2", provider.getRegion())
    }

    @Test
    fun providesCustomRegion() = runTest {
        val provider = StaticRegionProvider("eu-central-1")
        assertEquals("eu-central-1", provider.getRegion())
    }
}
