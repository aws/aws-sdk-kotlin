/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
// This package extends the smithy endpoints standard library with AWS-specific functions.
package aws.sdk.kotlin.runtime.endpoint.functions

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.endpoints.functions.isValidHostLabel
import aws.smithy.kotlin.runtime.net.isIpv4
import aws.smithy.kotlin.runtime.net.isIpv6

// the number of top-level components an arn contains (separated by colons)
private const val ARN_COMPONENT_COUNT = 6

private const val ARN_INDEX_PARTITION = 1
private const val ARN_INDEX_VENDOR = 2
private const val ARN_INDEX_REGION = 3
private const val ARN_INDEX_NAMESPACE = 4
private const val ARN_INDEX_RELATIVE_ID = 5

/**
 * Identifies the partition for the given AWS region.
 */
@InternalSdkApi
public fun partition(partitions: List<Partition>, region: String?): PartitionConfig? = region?.let {
    val explicitMatch = partitions.find { it.regions.contains(region) }
    if (explicitMatch != null) {
        return explicitMatch.baseConfig.mergeWith(explicitMatch.regions[region]!!)
    }

    val fallbackMatch = partitions.find { region.matches(it.regionRegex) }
        ?: partitions.find { it.id == "aws" }
    fallbackMatch?.baseConfig
}

/**
 * A partition defines a broader set of AWS regions.
 */
@InternalSdkApi
public data class Partition(
    public val id: String,
    /**
     * A mapping of known regions within this partition to region-specific configuration values.
     */
    public val regions: Map<String, PartitionConfig>,
    /**
     * A regular expression that can be used to identify arbitrary regions as part of this partition.
     */
    public val regionRegex: Regex,
    /**
     * The default configuration for this partition. Region-specific values in the [regions] map, if present, will
     * override these values when an explicit match is found during partitioning.
     */
    public val baseConfig: PartitionConfig,
)

/**
 * The core configuration details for a partition. This is the structure that endpoint providers interface receive as
 * the result of a partition call.
 */
@InternalSdkApi
public data class PartitionConfig(
    public val name: String? = null,
    public val dnsSuffix: String? = null,
    public val dualStackDnsSuffix: String? = null,
    public val supportsFIPS: Boolean? = null,
    public val supportsDualStack: Boolean? = null,
    public val implicitGlobalRegion: String? = null,
) {
    public fun mergeWith(other: PartitionConfig): PartitionConfig = PartitionConfig(
        other.name ?: name,
        other.dnsSuffix ?: dnsSuffix,
        other.dualStackDnsSuffix ?: dualStackDnsSuffix,
        other.supportsFIPS ?: supportsFIPS,
        other.supportsDualStack ?: supportsDualStack,
        other.implicitGlobalRegion ?: implicitGlobalRegion,
    )
}

/**
 * Splits an ARN into its component parts.
 *
 * The resource identifier is further split based on the type or scope delimiter present (if any).
 */
@InternalSdkApi
public fun parseArn(value: String?): Arn? = value?.let {
    val split = it.split(':', limit = ARN_COMPONENT_COUNT)
    if (split[0] != "arn") return null
    if (split.size != ARN_COMPONENT_COUNT) return null
    if (split[ARN_INDEX_PARTITION].isEmpty() || split[ARN_INDEX_VENDOR].isEmpty() || split[ARN_INDEX_RELATIVE_ID].isEmpty()) return null

    return Arn(
        split[ARN_INDEX_PARTITION],
        split[ARN_INDEX_VENDOR],
        split[ARN_INDEX_REGION],
        split[ARN_INDEX_NAMESPACE],
        split[ARN_INDEX_RELATIVE_ID].split(':', '/'),
    )
}

/**
 * Represents a parsed form of an ARN (Amazon Resource Name).
 */
@InternalSdkApi
public data class Arn(
    public val partition: String,
    public val service: String,
    public val region: String,
    public val accountId: String,
    public val resourceId: List<String>,
)

/**
 * Evaluates whether a string is a DNS-compatible bucket name that can be used with virtual hosted-style addressing.
 */
@InternalSdkApi
public fun isVirtualHostableS3Bucket(value: String?, allowSubdomains: Boolean): Boolean = value?.let {
    if (!isValidHostLabel(value, allowSubdomains)) {
        return false
    }

    if (!allowSubdomains) {
        value.isVirtualHostableS3Segment()
    } else {
        value.split('.').all(String::isVirtualHostableS3Segment)
    }
} ?: false

private fun String.isVirtualHostableS3Segment(): Boolean = length in 3..63 && none { it in 'A'..'Z' } && !isIpv4() && !isIpv6()
