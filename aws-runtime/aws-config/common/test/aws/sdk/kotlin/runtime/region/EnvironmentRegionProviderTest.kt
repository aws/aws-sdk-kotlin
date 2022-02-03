/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentRegionProviderTest {

    @Test
    fun noRegion() = runTest {
        val environ = mapOf<String, String>()
        val provider = EnvironmentRegionProvider { environ[it] }
        assertNull(provider.getRegion())
    }

    @Test
    fun providesRegion() = runTest {
        val environ = mapOf(
            "AWS_REGION" to "us-east-1"
        )

        val provider = EnvironmentRegionProvider { environ[it] }

        assertEquals("us-east-1", provider.getRegion())
    }
}
