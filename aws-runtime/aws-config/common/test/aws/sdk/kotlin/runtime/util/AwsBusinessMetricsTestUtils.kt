/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.util

import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.mutableAttributes
import aws.smithy.kotlin.runtime.collections.toMutableAttributes

/**
 * [Attributes] and any [BusinessMetric] that should be included.
 */
internal fun testAttributes(attributes: Attributes? = null, vararg metrics: BusinessMetric): Attributes {
    val testAttributes = attributes?.toMutableAttributes() ?: mutableAttributes()
    metrics.forEach { metric ->
        testAttributes.emitBusinessMetric(metric)
    }
    return testAttributes
}

/**
 * [Attributes] that only contain the specified [BusinessMetric].
 */
internal fun testAttributes(vararg metrics: BusinessMetric): Attributes {
    val testAttributes = mutableAttributes()
    metrics.forEach { metric ->
        testAttributes.emitBusinessMetric(metric)
    }
    return testAttributes
}

/**
 * Converts a [String] into an [AwsBusinessMetric.Credentials] if the identifier matches
 */
internal fun String.toAwsCredentialsBusinessMetric(): BusinessMetric =
    AwsBusinessMetric.Credentials.entries.find { it.identifier == this } ?: throw Exception("String '$this' is not an AWS business metric")
