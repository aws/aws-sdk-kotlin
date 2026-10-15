# CloudWatch Metrics for the AWS SDK for Kotlin

Publishes AWS SDK for Kotlin operational metrics - call duration, retry counts, error rates -
directly to Amazon CloudWatch using `PutMetricData`.

No OpenTelemetry dependency, no OTel collector, and no CloudWatch agent. Unlike the EMF provider,
which writes to stdout and relies on the surrounding platform to ingest it, this exporter publishes
over the wire and therefore works anywhere the SDK runs: EC2 without the agent, on-premises,
containers, desktop JVM, Android, and Kotlin/Native.

## Installation

```kotlin
dependencies {
    implementation("aws.sdk.kotlin:telemetry-cloudwatch:$VERSION")
}
```

## Quick start

```kotlin
AggregatingTelemetryProvider {
    exporter = CloudWatchMetricExporter {
        // Set this per application. Sharing the default namespace across applications merges
        // their metrics under one name, and there is no way to separate them afterwards.
        namespace = "MyApp/S3"
    }
    // Attributes promoted to CloudWatch dimensions. This is also the default, shown here because it
    // is the setting most worth understanding: service + operation are safe because both are bounded
    // by what you compiled against, and every attribute added beyond them multiplies the number of
    // billable metrics.
    dimensions = setOf("rpc.service", "rpc.method")
}.use { telemetry ->
    // `use` on the provider, not just the client: the provider owns the background collection
    // coroutine and the final flush. See the note below.
    S3Client { telemetryProvider = telemetry }.use { s3 ->
        s3.listBuckets()
    }
}
```

> **Close the provider you construct.** It owns a background collection coroutine, so it must be
> closed for that coroutine to stop; `use { }` is the safest form. As with every other closeable SDK
> dependency, ownership follows creation: because the provider above is built by *you* and assigned
> with `telemetryProvider = ...`, closing a client does not close it. Providers the SDK constructs on
> your behalf are reference-counted and closed with the last client using them, so it is always safe
> to share one provider across several clients - and across `withConfig` copies - in either case.

## Required IAM permission

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "cloudwatch:PutMetricData",
      "Resource": "*"
    }
  ]
}
```

`PutMetricData` does not support resource-level permissions; scope it with the
`cloudwatch:namespace` condition key if you need to restrict it.

## Configuration

Settings live on the object that owns the behaviour: aggregation on `AggregatingTelemetryProvider`,
publishing on `CloudWatchMetricExporter`, and scheduling on the reader.

| Setting | On | Default | Notes |
|---|---|---|---|
| `namespace` | exporter | `AwsSdk/KotlinSdk` | 1-255 ASCII characters; avoid the reserved `AWS/` prefix ([`Namespace`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html#API_PutMetricData_RequestParameters)) |
| `client` | exporter | one built from ambient credentials and region | Supply your own for different credentials, a different region, or a custom endpoint |
| `maxCallsPerUpload` | exporter | 10 | Hard cap on [`PutMetricData`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html) calls per cycle |
| `storageResolution` | exporter | 60 | `1` for [high-resolution metrics](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html#high-resolution-metrics), at higher cost |
| `dimensions` | provider | `rpc.service`, `rpc.method` | Attribute names promoted to CloudWatch [dimensions](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_Dimension.html); max 30 per metric |
| `detailedMetrics` | provider | none | Instruments that publish full distributions ([`Values`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Values)/[`Counts`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Counts)) instead of a [`StatisticSet`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_StatisticSet.html), enabling percentiles |
| `maxCardinality` | provider | 1000 | Distinct attribute sets per instrument before overflow bucketing |
| `flushMode` | provider or reader | periodic 1 min, or on-demand under Lambda | See below |
| `metricReader` | provider | a `PeriodicMetricReader` over `exporter` | Set this instead of `exporter` to publish to more than one destination, or on more than one interval |

Metrics are published as [`MetricDatum`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html)
objects via [`PutMetricData`](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_PutMetricData.html).
Counters become a single `Value`; histograms become either a `StatisticSet` or `Values`/`Counts` pairs
depending on `detailedMetrics`.

## Multiple destinations or intervals

`exporter` is shorthand for one destination on the default interval. For anything else, configure
readers explicitly - each carries its own exporter and its own interval:

```kotlin
AggregatingTelemetryProvider {
    metricReader = PeriodicMetricReader(interval = 5.minutes) {
        exporter = CloudWatchMetricExporter { namespace = "MyApp/S3" }
    }
}
```

`exporter` and `metricReader` are mutually exclusive; setting both fails at construction rather than
leaving one of them silently publishing nothing.

## Cost control

CloudWatch bills per custom metric, per API request, and per unique dimension combination. The
defaults are deliberately conservative; each of these knobs increases cost:

- **`dimensions`** - every distinct combination is a separate billable metric. Avoid unbounded
  values. Error codes and endpoints are the usual traps: they are influenced by remote responses, so
  they are excluded by default and should be added only if you understand the cardinality.
- **`detailedMetrics`** - publishes individual values instead of a summary. Enables p90/p99, but
  costs more requests and more memory. Enable it on the one or two instruments you actually chart.
- **`storageResolution = 1`** - high-resolution metrics cost more than standard.
- **`maxCallsPerUpload`** - the safety valve. If you see "maxCallsPerUpload exceeded" warnings,
  prefer reducing dimensions or `detailedMetrics` over raising the cap.

## Percentiles

Percentiles require the instrument to be listed in `detailedMetrics`, which is a provider setting
because the choice is about how measurements are *aggregated*, not how they are published:

```kotlin
AggregatingTelemetryProvider {
    exporter = CloudWatchMetricExporter { namespace = "MyApp/S3" }
    // Named individually, not enabled globally: a distribution's size grows with the number of
    // distinct recorded values, so this is the one instrument that pays for full fidelity.
    detailedMetrics = setOf("smithy.client.call.duration")
}
```

CloudWatch cannot compute percentiles from summary statistics - with the default settings only
count, sum, average, minimum, and maximum are available.

## Async instruments (gauges)

Gauges and async up/down counters are callback-based: the pipeline invokes the callback when it
collects, rather than the application pushing a value when something changes. Their values are
published absolutely - a queue depth reading of 7 then 3 publishes `7` then `3`, never a `-4` delta.

Sampling at collection time has two consequences, and both present as a *missing* metric rather than
an error:

- **Whatever owns the instrument must still be alive when the reader ticks.** Closing the owner stops
  its handles, and a stopped handle is not sampled. Because synchronous measurements recorded earlier
  in the interval are still published, the cycle logs as healthy while every async series it owned is
  absent. This includes the flush on `close()`, and idiomatic code walks into it: a handle stopped in
  a `finally` block, or a client closed by an inner `use { }`, is already stopped by the time the
  provider's closing flush runs.
- **Their attributes must be listed in `dimensions`.** Async instruments often carry attribute names
  unrelated to `rpc.*`. Attributes that are not listed are dropped, which merges series that differ
  only by the missing attribute into one.

The SDK's own async instruments are a worked example of both. They live on the HTTP engine, not the
operation layer, and the engine reads `telemetryProvider` from its *own* config - which defaults to
`TelemetryProvider.None`:

```kotlin
AggregatingTelemetryProvider {
    exporter = CloudWatchMetricExporter { namespace = "MyApp/S3" }
    // "state" is what the engine's gauges are keyed by (idle/acquired, queued/in-flight). Without it
    // the four series collapse into two.
    dimensions = setOf("rpc.service", "rpc.method", "state")
}.use { telemetry ->
    S3Client {
        // Instruments the operation layer: the smithy.client.call.* family.
        telemetryProvider = telemetry
        // Instruments the HTTP engine: the smithy.client.http.* family, which is where every async
        // instrument the SDK has lives. Setting it on the client does not reach the engine. The DSL
        // form leaves the engine's lifecycle with the SDK; assigning an instance makes it yours to
        // close.
        httpClient { telemetryProvider = telemetry }
    }.use { s3 ->
        s3.listBuckets()
        // Inside the client's scope, not after it: the connection-pool gauges are sampled here, so
        // closing the client first would drop them. One default interval's worth.
        delay(1.minutes)
    }
}
```

Under `FlushMode.OnDemand` a gauge reports its value once per `flush()`, so resolution equals flush
frequency rather than being continuous.

## AWS Lambda

Lambda freezes the execution environment between invocations, so a timer-based publish may never
fire. The provider detects Lambda via `AWS_LAMBDA_FUNCTION_NAME` and switches to on-demand
flushing; flush at the end of each invocation:

```kotlin
class Handler : RequestHandler<Input, Output> {
    // Fields, not locals: created once per execution environment and reused across invocations, so
    // the cost of building the client and provider lands on the cold start rather than every call.
    // No `flushMode` is set - Lambda is auto-detected and on-demand flushing is selected for you.
    private val telemetry = AggregatingTelemetryProvider {
        exporter = CloudWatchMetricExporter { namespace = "MyFunction" }
    }
    private val s3 = S3Client { telemetryProvider = telemetry }

    override fun handleRequest(input: Input, context: Context): Output = runBlocking {
        try {
            doWork(input)
        } finally {
            // In `finally`, so metrics from a failed invocation are still published - those are the
            // ones you most want. Note `flush()`, not `close()`: the provider outlives the
            // invocation and must stay usable for the next one.
            telemetry.flush()
        }
    }
}
```

Gauge resolution equals flush frequency in this mode. If your function is short-lived and
high-volume, consider the EMF provider instead - it has no background work at all.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| No metrics in CloudWatch | Provider never closed/flushed, or missing `cloudwatch:PutMetricData` |
| `AccessDeniedException` in logs | IAM policy above not attached |
| Percentiles unavailable | Instrument not listed in `detailedMetrics` |
| Gauges / async counters missing | Their owner was closed before the interval elapsed, or their attributes are not in `dimensions` |
| No `smithy.client.http.*` metrics | The HTTP engine was not instrumented - set `telemetryProvider` inside `httpClient { }` |
| "maxCallsPerUpload exceeded" warnings | Too many dimensions or detailed instruments |
| Metrics appear up to 15 min late | Expected on first publish: CloudWatch creates the metric |
| Rising memory | A `detailedMetrics` instrument with high-cardinality dimensions |

### Enabling the pipeline's own logs

The pipeline logs to the `loggerProvider` on its builders, which defaults to `LoggerProvider.None`.
**Until you set one, none of the diagnostics below are emitted** - not even warnings about dropped
data. This is deliberate: telemetry plumbing that logged by default would be noise for the majority
of applications that never need to debug it. It does mean "no output" is the expected state rather
than evidence that nothing went wrong.

Set it on both builders - they are configured separately because they log different things, and the
exporter is usable without the provider:

```kotlin
// aws.smithy.kotlin:logging-slf4j2 routes to whatever SLF4J backend the application already has.
AggregatingTelemetryProvider {
    loggerProvider = Slf4jLoggerProvider
    exporter = CloudWatchMetricExporter {
        namespace = "MyApp/S3"
        loggerProvider = Slf4jLoggerProvider
    }
}
```

Then set levels on the two logger names, which are the fully-qualified class names:

| Logger | Level | What you get |
|---|---|---|
| `aws.sdk.kotlin.runtime.telemetry.cloudwatch.CloudWatchMetricExporter` | `WARN` | Dropped datums: `maxCallsPerUpload` exceeded, `PutMetricData` failures |
| | `DEBUG` | One summary per publish cycle - datum, request, and instrument counts |
| | `TRACE` | One line per `PutMetricData` request |
| `aws.smithy.kotlin.runtime.telemetry.metrics.aggregation.AggregatingTelemetryProvider` | `WARN` | Cardinality overflow, collection failures, buggy-exporter throws |
| | `DEBUG` | One line per collection cycle, including cycles with nothing to publish |

`DEBUG` is the level to start at: its volume is fixed per interval rather than proportional to
traffic, so it is affordable to leave on in production while chasing an empty chart. `TRACE` scales
with the number of batches and is better suited to a local run.

If `DEBUG` shows cycles being collected and published but CloudWatch shows nothing, the problem is
downstream of this module - check the IAM permission and the namespace. If it shows no cycles at all,
the reader is not running: under `FlushMode.OnDemand` that is expected until you call `flush()`.
