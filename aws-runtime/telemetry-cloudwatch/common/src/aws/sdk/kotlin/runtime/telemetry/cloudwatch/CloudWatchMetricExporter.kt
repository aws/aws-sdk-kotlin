/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.telemetry.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatch.putMetricData
import aws.sdk.kotlin.services.cloudwatch.withConfig
import aws.smithy.kotlin.runtime.telemetry.TelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.LoggerProvider
import aws.smithy.kotlin.runtime.telemetry.logging.getLogger
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricData
import aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.MetricExporter
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException

/**
 * A [MetricExporter] that publishes metrics to CloudWatch. Requires the `cloudwatch:PutMetricData`
 * permission.
 *
 * CloudWatch bills per request and per custom metric, where a metric is a distinct name and
 * dimension-value combination. [Builder.maxCallsPerUpload] bounds requests per cycle; the dimension
 * allowlist and cardinality limit on `AggregatingTelemetryProvider` bound distinct metrics.
 *
 * Construct one per provider. This exporter owns its CloudWatch client and closes it in [shutdown], so a
 * shared instance stops publishing for every provider but the first to close.
 */
public class CloudWatchMetricExporter private constructor(builder: Builder) : MetricExporter {
    // Before any property initializer, so an invalid configuration cannot construct a client. Only settings
    // this exporter acts on itself are checked; namespace and resolution are left to CloudWatch to enforce.
    init {
        require(builder.maxCallsPerUpload > 0) { "maxCallsPerUpload must be positive" }
    }

    private val namespace: String = builder.namespace
    private val maxCallsPerUpload: Int = builder.maxCallsPerUpload
    private val chunker = MetricDatumChunker(builder.storageResolution)
    private val logger: Logger = builder.loggerProvider.getLogger<CloudWatchMetricExporter>()

    /**
     * Client used to publish. Always owned by this exporter and closed by [shutdown].
     *
     * Telemetry is forced off. Metrics emitted by `PutMetricData` would otherwise be collected on the
     * next cycle and published, producing a feedback loop that does not decay. A caller-supplied
     * client is copied via `withConfig`; the original is untouched and remains theirs to close.
     */
    private val client: CloudWatchClient = builder.client
        ?.withTelemetryDisabled()
        ?: CloudWatchClient { telemetryProvider = TelemetryProvider.None }

    private val closed = atomic(false)

    /** Converts, batches, and publishes one collection cycle. Publish failures are logged, not thrown. */
    override suspend fun export(metrics: List<MetricData>) {
        val requests = chunker.toRequests(chunker.toDatums(metrics))
        val allowed = requests.take(maxCallsPerUpload)

        if (requests.size > maxCallsPerUpload) {
            val dropped = requests.drop(maxCallsPerUpload).sumOf { it.size }
            logger.warn {
                "maxCallsPerUpload=$maxCallsPerUpload exceeded; dropped $dropped metric datum(s). " +
                    "Reduce dimensions or detailedMetrics, or raise the cap."
            }
        }

        allowed.forEach { batch ->
            try {
                client.putMetricData {
                    namespace = this@CloudWatchMetricExporter.namespace
                    metricData = batch
                }
                logger.trace { "published ${batch.size} datum(s) to $namespace" }
            } catch (e: CancellationException) {
                throw e // shutdown, not a publish failure
            } catch (e: Exception) {
                // Batches are independent; one throttled request should not discard the rest.
                logger.warn(e) { "PutMetricData failed; dropped ${batch.size} datum(s)" }
            }
        }

        logger.debug {
            "cycle published ${allowed.sumOf { it.size }} datum(s) in ${allowed.size} request(s) " +
                "for ${metrics.size} instrument(s) to $namespace"
        }
    }

    /** Closes the client. Idempotent. Called by the reader after its final flush. */
    override suspend fun shutdown() {
        if (!closed.compareAndSet(expect = false, update = true)) return
        client.close()
    }

    public companion object {
        /** Not prefixed `AWS/`, which is reserved for metrics published by AWS services. */
        internal const val DEFAULT_NAMESPACE = "AwsSdk/KotlinSdk"

        /** Matches the AWS SDK for Java v2 `maximumCallsPerUpload` default. */
        internal const val DEFAULT_MAX_CALLS_PER_UPLOAD = 10

        /** Standard resolution: one datapoint per minute. */
        internal const val STANDARD_RESOLUTION = 60

        public operator fun invoke(block: Builder.() -> Unit = {}): CloudWatchMetricExporter = CloudWatchMetricExporter(Builder().apply(block))
    }

    public class Builder {
        /**
         * CloudWatch namespace. Set this per application; metrics from applications sharing a namespace
         * cannot be attributed afterwards.
         */
        public var namespace: String = DEFAULT_NAMESPACE

        /**
         * Client used to publish. Defaults to one created from ambient credentials and region. Supply
         * your own for different credentials, a different region, or a custom endpoint. The exporter
         * publishes with a copy and closes only that copy.
         */
        public var client: CloudWatchClient? = null

        /**
         * Hard cap on `PutMetricData` calls per collection cycle. Excess datums are dropped with a
         * warning naming this setting. Hitting the cap usually indicates unintended cardinality.
         */
        public var maxCallsPerUpload: Int = DEFAULT_MAX_CALLS_PER_UPLOAD

        /**
         * 1 for high-resolution metrics (sub-minute granularity, higher cost), 60 for standard. Only
         * useful as 1 when the reader's publish interval is also sub-minute.
         */
        public var storageResolution: Int = STANDARD_RESOLUTION

        /**
         * Where this exporter's own diagnostics go. Defaults to `None`.
         *
         * Do not pass a provider that logs through the SDK to CloudWatch Logs; that reintroduces the
         * feedback loop on the logging path.
         */
        public var loggerProvider: LoggerProvider = LoggerProvider.None
    }
}

/**
 * Returns a copy of this client with telemetry disabled. The copy shares the HTTP engine and
 * credentials provider by reference count and must be closed by whoever created it.
 */
private fun CloudWatchClient.withTelemetryDisabled(): CloudWatchClient = withConfig { telemetryProvider = TelemetryProvider.None }
