/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.interceptors.businessmetrics

import aws.sdk.kotlin.runtime.http.BUSINESS_METRICS_MAX_LENGTH
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.copy
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.collections.MutableAttributes
import aws.smithy.kotlin.runtime.collections.toMutableAttributes
import aws.smithy.kotlin.runtime.telemetry.logging.Logger

/**
 * Makes sure the metrics do not exceed the maximum size and truncates them if so.
 * Makes sure that metric identifiers are not > 2 chars in length. Skips them if so.
 */
internal fun formatMetrics(metrics: MutableSet<BusinessMetric>, logger: Logger): String {
    val allowedMetrics = metrics.filter {
        if (it.identifier.length > 2) {
            logger.warn {
                "Business metric '${it.identifier}' will be skipped due to length being > 2. " +
                    "This is likely a bug. Please raise an issue at https://github.com/awslabs/aws-sdk-kotlin/issues/new/choose"
            }
            false
        } else {
            true
        }
    }
    if (allowedMetrics.isEmpty()) {
        return ""
    }
    val metricsString = allowedMetrics.joinToString(",", "m/") { it.identifier }
    val metricsByteArray = metricsString.encodeToByteArray()

    if (metricsByteArray.size <= BUSINESS_METRICS_MAX_LENGTH) return metricsString

    val lastCommaIndex = metricsByteArray
        .sliceArray(0 until 1024)
        .indexOfLast { it == ','.code.toByte() }
        .takeIf { it != -1 }

    lastCommaIndex?.let {
        return metricsByteArray.decodeToString(
            0,
            lastCommaIndex,
            true,
        )
    }

    throw IllegalStateException("Business metrics are incorrectly formatted: $metricsString")
}

/**
 * AWS SDK specific business metrics
 */
@InternalApi
public enum class AwsBusinessMetric(public override val identifier: String) : BusinessMetric {
    S3_EXPRESS_BUCKET("J"),
    DDB_MAPPER("d"),
    ;

    @InternalApi
    public enum class Credentials(public override val identifier: String) : BusinessMetric {
        CREDENTIALS_CODE("e"),
        CREDENTIALS_JVM_SYSTEM_PROPERTIES("f"),
        CREDENTIALS_ENV_VARS("g"),
        CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN("h"),
        CREDENTIALS_STS_ASSUME_ROLE("i"),
        CREDENTIALS_STS_ASSUME_ROLE_WEB_ID("k"),
        CREDENTIALS_PROFILE("n"),
        CREDENTIALS_PROFILE_SOURCE_PROFILE("o"),
        CREDENTIALS_PROFILE_NAMED_PROVIDER("p"),
        CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN("q"),
        CREDENTIALS_PROFILE_SSO("r"),
        CREDENTIALS_SSO("s"),
        CREDENTIALS_PROFILE_SSO_LEGACY("t"),
        CREDENTIALS_SSO_LEGACY("u"),
        CREDENTIALS_PROFILE_PROCESS("v"),
        CREDENTIALS_PROCESS("w"),
        CREDENTIALS_HTTP("z"),
        CREDENTIALS_IMDS("0"),
    }

    override fun toString(): String = identifier
}

/**
 * Emits a business metric into [Credentials.attributes]
 * @param metric The [BusinessMetric] to be emitted.
 */
@InternalApi
public fun Credentials.withBusinessMetric(metric: BusinessMetric): Credentials =
    when (val credentialsAttributes = this.attributes) {
        is MutableAttributes -> {
            credentialsAttributes.emitBusinessMetric(metric)
            this
        }
        else -> {
            val newCredentialsAttributes = credentialsAttributes.toMutableAttributes()
            newCredentialsAttributes.emitBusinessMetric(metric)
            this.copy(attributes = newCredentialsAttributes)
        }
    }

/**
 * Emits business metrics into [Credentials.attributes]
 * @param metrics The [BusinessMetric]s to be emitted.
 */
@InternalApi
public fun Credentials.withBusinessMetrics(metrics: Set<BusinessMetric>): Credentials {
    var credentials = this
    metrics.forEach { metric ->
        credentials = this.withBusinessMetric(metric)
    }
    return credentials
}
