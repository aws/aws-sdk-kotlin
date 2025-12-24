/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.util.TestInstanceMetadataProvider
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRegionProviderChainTest {
    private data class RegionProviderChainTest(
        val name: String,
        val platformProvider: TestPlatformProvider,
        val instanceMetadataProvider: TestInstanceMetadataProvider,
        val region: String?,
        val targets: List<String> = emptyList(),
    )

    @Test
    fun testSuite() = runTest {
        val tests = Json.parseToJsonElement(REGION_PROVIDER_CHAIN_TEST_SUITE).jsonArray
            .map { it.jsonObject }
            .map {
                val name = it["name"]!!.jsonPrimitive.content
                val platform = TestPlatformProvider.fromJsonNode(it["platform"]!!.jsonObject)
                val instanceMetadata = TestInstanceMetadataProvider.fromJsonNode(it["imds"]!!.jsonObject)
                val region = it["region"]!!.jsonPrimitive.contentOrNull
                RegionProviderChainTest(name, platform, instanceMetadata, region)
            }

        tests.forEach { test ->
            val provider = DefaultRegionProviderChain(
                platformProvider = test.platformProvider,
                imdsClient = lazy { test.instanceMetadataProvider },
            )
            val actual = provider.getRegion()
            assertEquals(test.region, actual, test.name)
        }
    }
}

/**
 * Construct a [TestPlatformProvider] from a JSON node like:
 *
 * ```json
 * {
 *     "env": {
 *         "ENV_VAR": "value"
 *     },
 *     "props": {
 *         "aws.property": "value"
 *     },
 *     "fs": {
 *         "filename": "contents"
 *     }
 * }
 * ```
 */
fun TestPlatformProvider.Companion.fromJsonNode(obj: JsonObject): TestPlatformProvider {
    val env = obj["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    val props = obj["props"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    val fs = obj["fs"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    return TestPlatformProvider(env, props, fs)
}

/**
 * Construct a [TestInstanceMetadataProvider] from a JSON object containing metadata as key-value pairs.
 */
fun TestInstanceMetadataProvider.Companion.fromJsonNode(obj: JsonObject): TestInstanceMetadataProvider {
    val metadata = obj.jsonObject.mapValues { it.value.jsonPrimitive.content }
    return TestInstanceMetadataProvider(metadata)
}

// language=JSON
private const val REGION_PROVIDER_CHAIN_TEST_SUITE = """
[
    {
        "name": "no region configured",
        "platform": {
            "env": {},
            "props": {},
            "fs": {}
        },
        "imds": {},
        "region": null
    },
    {
        "name": "environment configured",
        "platform": {
            "env": {
                "AWS_REGION": "us-east-2"
            },
            "props": {},
            "fs": {}
        },
        "imds": {},
        "region": "us-east-2"
    },
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
        "name": "default profile",
        "platform": {
            "env": {
              "AWS_CONFIG_FILE": "config"
            },
            "props": {},
            "fs": {
              "config": "[default]\nregion = us-east-2"
            }
        },
        "imds": {},
        "region": "us-east-2"
    },
    {
        "name": "explicit profile",
        "platform": {
            "env": {
              "AWS_CONFIG_FILE": "config",
              "AWS_PROFILE": "test-profile"
            },
            "props": {},
            "fs": {
              "config": "[default]\nregion = us-east-2\n[profile test-profile]\nregion = us-west-1"
            }
        },
        "imds": {},
        "region": "us-west-1"
    },
    {
        "name": "imds configured",
        "platform": {
            "env": {},
            "props": {},
            "fs": {}
        },
        "imds": {
            "/latest/meta-data/placement/region": "us-east-1"
        },
        "region": "us-east-1"
    },
    {
        "name": "jvm system properties are favored over imds",
        "platform": {
            "env": {
                "AWS_REGION": "us-east-2"
            },
            "props": {},
            "fs": {}
        },
        "imds": {
            "/latest/meta-data/placement/region": "us-east-1"
        },
        "region": "us-east-2"
    },
    {
        "name": "environment variables are favored over imds",
        "platform": {
            "env": {},
            "props": {
                "aws.region": "us-west-1"
            },
            "fs": {}
        },
        "imds": {
            "/latest/meta-data/placement/region": "us-east-1"
        },
        "region": "us-west-1"
    },
    {
        "name": "profile is favored over imds",
        "platform": {
            "env": {
              "AWS_CONFIG_FILE": "config"
            },
            "props": {},
            "fs": {
              "config": "[default]\nregion = us-west-2"
            }
        },
        "imds": {
            "/latest/meta-data/placement/region": "us-east-1"
        },
        "region": "us-west-2"
    }
]
"""
