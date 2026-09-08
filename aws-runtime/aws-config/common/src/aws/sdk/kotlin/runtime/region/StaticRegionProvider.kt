/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.smithy.kotlin.runtime.client.region.RegionProvider

/**
 * [RegionProvider] that always returns a static, pre-configured region.
 */
public class StaticRegionProvider(private val region: String) : RegionProvider {
    override suspend fun getRegion(): String = region
}
