/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

/**
 * Interface for providing AWS region information. Implementations are free to use any strategy for
 * providing region information
 */
public interface AwsRegionProvider {
    /**
     * Return the region name to use. If region information is not available, implementations should return null
     */
    public suspend fun getRegion(): String?
}
