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
 * A [MetricExporter] that publishes to CloudWatch with `PutMetricData`.
 *
 * Requires no ambient log ingestion, no CloudWatch agent, and no collector — unlike an EMF-based
 * approach it works anywhere the SDK runs. Publishing requires the `cloudwatch:PutMetricData`
 * permission.
 *
 * Defaults follow the AWS SDK for Java v2 `CloudWatchMetricPublisher`: summary statistics for every
 * metric, percentiles opt-in per instrument, and a hard cap on requests per cycle.
 *
 * ## Cost
 *
 * `PutMetricData` is billed per request and custom metrics are billed per metric-month, where a
 * "metric" is a distinct name + dimension-value combination. Both axes are bounded by design:
 * `maxCallsPerUpload` here bounds requests, and `maxCardinality` plus the dimension allowlist on
 * `SdkTelemetryProvider` bound distinct metrics. Users who widen the dimension set are the ones who
 * multiply their bill, which is why dimensions are opt-in rather than "everything available".
 *
 * ## Thread safety
 *
 * Not called concurrently in normal use: `PeriodicMetricReader` serialises collect-then-export in a
 * single coroutine, and every field here is immutable after construction. If a future reader
 * overlaps exports, only the underlying client's own concurrency guarantees apply — which are fine,
 * because this class holds no mutable state.
 */
public class CloudWatchMetricExporter private constructor(builder: Builder) : MetricExporter {
    private val namespace: String = builder.namespace
    private val maxCallsPerUpload: Int = builder.maxCallsPerUpload
    private val chunker = MetricDatumChunker(builder.storageResolution)

    // getLogger<T>() rather than a literal name: it resolves to the fully-qualified class name. SLF4J and
    // log4j2 treat logger names as a dot-delimited hierarchy, so a bare "CloudWatchMetricExporter" cannot be
    // matched by a level set on this module's package — which is how log configuration is normally written.
    private val logger: Logger = builder.loggerProvider.getLogger<CloudWatchMetricExporter>()

    /**
     * Recursion guard: telemetry is forced off on the client used for publishing.
     *
     * Without this, each `PutMetricData` call generates SDK metrics of its own, which are collected
     * on the next cycle, which produces datums, which trigger another `PutMetricData` — a feedback
     * loop that never settles and bills for every turn of it. Note that this is *not* self-limiting:
     * publishing N datums does not produce fewer than N datums next cycle, so the loop does not
     * decay.
     *
     * This is enforced here rather than documented as a caveat because the failure is expensive,
     * silent, and easy to cause accidentally — a user who passes their application's ordinary,
     * telemetry-enabled CloudWatch client would otherwise trip it with no warning.
     *
     * ## Ownership
     *
     * **This exporter always owns whatever ends up in this field**, and [shutdown] always closes it.
     * That holds in both branches below, which is easy to get wrong:
     *
     * - Nothing supplied: we construct a client outright. Plainly ours.
     * - A client supplied: we do *not* store the caller's instance. `withTelemetryDisabled()` returns
     *   a `withConfig` **copy**, which per the `withConfig` contract "exists independently of the
     *   original [and] has its own lifetime and should be closed when no longer needed". The copy is
     *   ours; the caller's original is untouched and remains theirs.
     *
     * Closing the copy cannot harm the caller: `withConfig` shares managed resources by reference
     * count, so releasing the copy's share leaves the original holding its own. **Not** closing the
     * copy is the actual hazard — the caller's share count would never reach zero, and their own
     * `close()` would silently fail to release the HTTP engine and credentials provider.
     */
    private val client: CloudWatchClient = builder.client
        ?.withTelemetryDisabled()
        ?: CloudWatchClient { telemetryProvider = TelemetryProvider.None }

    /**
     * Guards [shutdown] against running twice.
     *
     * Reachable by two paths — `SdkTelemetryProvider.close()` via the reader, and a direct
     * `shutdown()` — so idempotence is required rather than merely tidy.
     */
    private val closed = atomic(false)

    /**
     * Set when this exporter is installed into a provider. Never cleared.
     *
     * Deliberately one-way, and deliberately *not* a share count. Reference counting is the right
     * answer when several owners legitimately hold one resource — an HTTP connection pool, say — and
     * the wrong answer here, because there is no legitimate reason to point two providers at one
     * exporter. Doing so would run two collection loops against one namespace on independent
     * intervals, and would add together delta sums produced by two separate aggregators. So: fail,
     * rather than count.
     */
    private val attached = atomic(false)

    /**
     * Reject a second attachment.
     *
     * Throws rather than logs, unlike every other error path in this class. The distinction: a failed
     * publish loses one interval of data, whereas a shared exporter produces numbers that are
     * *wrong but plausible* indefinitely. The former is worth surviving, the latter is not.
     */
    override fun onAttach() {
        check(attached.compareAndSet(expect = false, update = true)) {
            "CloudWatchMetricExporter instances cannot be shared between SdkTelemetryProvider " +
                "instances. Construct one exporter per provider."
        }
    }

    /**
     * Convert, batch, and publish one collection cycle.
     *
     * Never throws for a publish failure — see the `catch` below.
     */
    override suspend fun export(metrics: List<MetricData>) {
        val requests = chunker.toRequests(chunker.toDatums(metrics))
        val allowed = requests.take(maxCallsPerUpload)

        if (requests.size > maxCallsPerUpload) {
            // Drop rather than raise the cap on the fly: the cap exists to make cost predictable, so
            // silently exceeding it under load would defeat its only purpose. Dropping is also
            // preferable to blocking, which would push back on the collection loop and eventually
            // on the application. WARN, not ERROR — data is lost, but nothing is broken, and the
            // message says which knob fixes it.
            val dropped = requests.drop(maxCallsPerUpload).sumOf { it.size }
            logger.warn {
                "maxCallsPerUpload=$maxCallsPerUpload exceeded; dropped $dropped metric datum(s). " +
                    "Reduce dimensions or detailedMetrics, or raise the cap."
            }
        }

        // Sequential, not concurrent: batches are few (bounded by maxCallsPerUpload), and issuing
        // them serially keeps telemetry from competing with the application for connections in the
        // shared HTTP pool. Publishing latency does not matter here — nothing is waiting on it.
        allowed.forEach { batch ->
            try {
                client.putMetricData {
                    namespace = this@CloudWatchMetricExporter.namespace
                    metricData = batch
                }
                logger.trace { "published ${batch.size} datum(s) to $namespace" }
            } catch (e: CancellationException) {
                // Cancellation is not a failure, it is the reader shutting down or timing out.
                // Swallowing it would break structured concurrency: the coroutine would keep
                // running after its scope was cancelled, and `close()` could hang.
                throw e
            } catch (e: Exception) {
                // Log and drop. A telemetry failure must never surface in a user's SDK call — the
                // application asked to observe its behaviour, not to depend on CloudWatch. Continue
                // the loop rather than returning: the remaining batches are independent, and one
                // throttled request should not discard the rest of the cycle.
                logger.warn(e) { "PutMetricData failed; dropped ${batch.size} datum(s)" }
            }
        }

        // One summary per cycle at DEBUG, versus one line per request at TRACE above. The distinction is
        // volume: TRACE scales with traffic, this does not, so it stays affordable to leave on in a
        // production deployment that is trying to find out why a chart is empty.
        logger.debug {
            "cycle published ${allowed.sumOf { it.size }} datum(s) in ${allowed.size} request(s) " +
                "for ${metrics.size} instrument(s) to $namespace"
        }
    }

    /**
     * Close the client. Idempotent.
     *
     * Called by the reader after its final flush, so any buffered data has already been published by
     * the time the client goes away.
     *
     * Unconditional, per the ownership note on [client]: in both the self-constructed and the
     * caller-supplied case, the object in that field is one this exporter created.
     */
    override suspend fun shutdown() {
        if (!closed.compareAndSet(expect = false, update = true)) return
        client.close()
    }

    public companion object {
        /**
         * Default namespace. Slash-separated to group under a vendor prefix in the console, matching
         * the convention AWS services use (`AWS/EC2`, `AWS/Lambda`). Not `AWS/...` — that prefix is
         * reserved for service-published metrics, and reusing it would mix SDK client-side metrics
         * in with the service's own server-side view of the same calls.
         */
        internal const val DEFAULT_NAMESPACE = "AwsSdk/KotlinSdk"

        /**
         * Default request cap per cycle, from Java v2's `maximumCallsPerUpload`. Ten requests of up
         * to 1000 datums is 10,000 datums per cycle — far more than a sane cardinality budget
         * produces, so the cap should never bind in a correctly configured deployment. It exists as
         * a backstop against a cardinality explosion turning into an unbounded bill.
         */
        internal const val DEFAULT_MAX_CALLS_PER_UPLOAD = 10

        /**
         * Standard resolution: one datapoint per minute. The default because high-resolution metrics
         * cost more and are only useful when the publish interval is also sub-minute.
         */
        internal const val STANDARD_RESOLUTION = 60

        /**
         * Builder-style factory. `invoke` on the companion so construction reads as
         * `CloudWatchMetricExporter { namespace = "MyApp" }`, matching how generated SDK clients and
         * shapes are constructed elsewhere in the SDK.
         *
         * [Builder.validate] runs here, at construction, rather than at first export: a bad
         * namespace should fail where the mistake is, not fifteen minutes later inside a background
         * coroutine whose exception is swallowed.
         */
        public operator fun invoke(block: Builder.() -> Unit = {}): CloudWatchMetricExporter = CloudWatchMetricExporter(Builder().apply(block).also { it.validate() })
    }

    public class Builder {
        /**
         * CloudWatch namespace — the top-level grouping for these metrics. Must be 1–255 characters.
         *
         * Set this per application when several applications share an account, otherwise their
         * metrics merge under one name and become impossible to attribute.
         */
        public var namespace: String = DEFAULT_NAMESPACE

        /**
         * Client used to publish. Defaults to one created from ambient credentials and region.
         *
         * Supply your own when publishing needs different credentials, a different region (for
         * example, centralising metrics in one account), or a custom endpoint for testing. Telemetry
         * is forced off on a copy — see the recursion guard — so the client you pass stays yours to
         * close, and the exporter closes only its own copy of it.
         */
        public var client: CloudWatchClient? = null

        /**
         * Hard cap on `PutMetricData` calls per collection cycle. Excess datums are dropped with a
         * warning.
         *
         * Together with the publish interval this puts a ceiling on request volume, and therefore on
         * spend, that no amount of application load can exceed. Raise it only after checking why the
         * cap is being hit: the usual cause is unintended cardinality, and raising the cap converts
         * a dropped-data warning into a larger bill without fixing anything.
         */
        public var maxCallsPerUpload: Int = DEFAULT_MAX_CALLS_PER_UPLOAD

        /**
         * 1 for high-resolution metrics (sub-minute granularity, higher cost), 60 for standard.
         *
         * Only worth 1 if the reader's publish interval is also sub-minute — otherwise you pay the
         * high-resolution rate for data that still arrives once a minute.
         */
        public var storageResolution: Int = STANDARD_RESOLUTION

        /**
         * Where this exporter's own diagnostics go. Defaults to `None` so telemetry stays silent
         * unless asked otherwise.
         *
         * Worth wiring up when metrics do not appear as expected: the WARN messages in [export]
         * distinguish "dropped by the cap" from "rejected by CloudWatch", which is otherwise
         * invisible. Do NOT pass the `loggerProvider` of a provider that logs *through* the SDK to
         * CloudWatch Logs — that reintroduces the recursion this class guards against, on the
         * logging path instead of the metrics path.
         */
        public var loggerProvider: LoggerProvider = LoggerProvider.None

        /**
         * Fail fast on configuration that CloudWatch would reject at request time.
         *
         * Each check mirrors a documented API constraint, so a violation is a guaranteed rejection
         * rather than a judgement call — worth an exception at construction. `storageResolution` in
         * particular accepts only the two literal values; there is no continuum, and a plausible
         * guess like 30 is simply invalid.
         *
         * DELIBERATELY NOT CHECKED: the namespace pattern (`[^:].*`, ASCII, no control characters) and
         * the `AWS/` prefix. The pattern is cheap to add and arguably should be; the `AWS/` prefix is
         * documented as "should not" rather than "must not", so rejecting it would be stricter than the
         * service and would break anyone deliberately writing there.
         *
         * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html#API_PutMetricData_RequestParameters">PutMetricData request parameters</a>
         * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-StorageResolution">MetricDatum.StorageResolution</a>
         */
        internal fun validate() {
            require(namespace.isNotEmpty() && namespace.length <= CloudWatchLimits.MAX_NAMESPACE_LENGTH) {
                "namespace must be 1..${CloudWatchLimits.MAX_NAMESPACE_LENGTH} characters, got ${namespace.length}"
            }
            require(maxCallsPerUpload > 0) { "maxCallsPerUpload must be positive" }
            require(storageResolution == 1 || storageResolution == 60) {
                "storageResolution must be 1 or 60, got $storageResolution"
            }
        }
    }
}

/**
 * Returns a copy of this client with telemetry disabled — see the recursion guard on
 * `CloudWatchMetricExporter.client`.
 *
 * `withConfig` copies configuration and shares the underlying HTTP engine and credentials provider by
 * reference count, so this is cheap and does not double the client's resource footprint.
 *
 * The copy has **its own lifetime** and must be closed by whoever created it — here, the exporter.
 * Leaving it unclosed would pin the original's managed resources at a non-zero share count, so the
 * caller's own `close()` would appear to succeed while the engine stayed open.
 */
private fun CloudWatchClient.withTelemetryDisabled(): CloudWatchClient = withConfig { telemetryProvider = TelemetryProvider.None }
