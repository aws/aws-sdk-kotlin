/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import kotlin.test.Test

class DefaultRegionProviderChainTestJVM {
    @Test
    fun testSuite() = runRegionProviderChainTestSuite(JVM_REGION_PROVIDER_CHAIN_TEST_SUITE)
}

// language=JSON
private const val JVM_REGION_PROVIDER_CHAIN_TEST_SUITE = """
[
    {
        "name": "jvm property is favored",
        "platform": {
            "env": {
                "AWS_REGION": "us-east-2"
            },
            "props": {
                "aws.region": "us-west-1"
            },
            "fs": {}
        },
        "imds": {},
        "region": "us-west-1"
    },
    {
        "name": "jvm system properties are favored over imds",
        "platform": {
            "env": {},
            "props": {
                "aws.region": "us-east-2"
            },
            "fs": {}
        },
        "imds": {
            "/latest/meta-data/placement/region": "us-east-1"
        },
        "region": "us-east-2"
    }
]
"""
