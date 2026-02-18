# Service benchmarks

This module is used for benchmarking the performance of generated clients against AWS services. The top 7 services (by
traffic coming from the AWS SDK for Kotlin) are tested and metrics are captured with summaries distilled after the runs
are complete

## Instructions

Ensure all services, including `iam`, have been generated before proceeding with the benchmarks. To run the benchmarks:
* `./gradlew build`
  This builds the whole SDK.
* `./gradlew :tests:benchmarks:service-benchmarks:jvmRun`
  This runs the benchmark suite and prints the results to the console formatted as a Markdown table.

## Baseline as of 1/5/2026

The following benchmark run serves as a baseline for future runs:

### Environment

| Instance type   | Operating system | SDK version |
|-----------------|------------------|-------------|
| EC2 m7i.4xlarge | Amazon Linux 2   | 1.5.113     |

### Results

|                       | Overhead (ms) |    n |   min |   avg |   med |   p90 |    p99 |     max |
| :---                  |          ---: | ---: |  ---: |  ---: |  ---: |  ---: |   ---: |    ---: |
| **S3**                |               |      |       |       |       |       |        |         |
|   —HeadObject         |               | 1439 | 0.428 | 0.777 | 0.640 | 0.921 |  6.642 |  14.399 |
|   —PutObject          |               |  753 | 0.345 | 0.716 | 0.627 | 0.839 |  2.814 |   9.736 |
| **SNS**               |               |      |       |       |       |       |        |         |
|   —GetTopicAttributes |               | 3826 | 0.196 | 0.469 | 0.314 | 0.457 | 10.428 |  13.473 |
|   —Publish            |               | 1233 | 0.188 | 0.434 | 0.269 | 0.426 | 10.143 |  11.385 |
| **STS**               |               |      |       |       |       |       |        |         |
|   —AssumeRole         |               |  998 | 0.334 | 0.535 | 0.489 | 0.648 |  0.875 |   9.221 |
|   —GetCallerIdentity  |               | 4392 | 0.169 | 0.321 | 0.285 | 0.345 |  0.458 |   9.468 |
| **CloudWatch**        |               |      |       |       |       |       |        |         |
|   —GetMetricData      |               | 1500 | 0.217 | 1.655 | 0.398 | 3.462 | 10.331 | 260.701 |
|   —PutMetricData      |               | 2536 | 0.125 | 1.163 | 0.173 | 3.989 |  8.552 | 509.896 |
| **CloudWatch Events** |               |      |       |       |       |       |        |         |
|   —DescribeEventBus   |               | 1500 | 0.215 | 0.433 | 0.316 | 0.513 |  2.714 |  12.463 |
|   —PutEvents          |               | 5318 | 0.138 | 0.268 | 0.171 | 0.235 |  2.707 |   8.775 |
| **DynamoDB**          |               |      |       |       |       |       |        |         |
|   —GetItem            |               | 4967 | 0.124 | 0.171 | 0.152 | 0.216 |  0.387 |   2.589 |
|   —PutItem            |               | 3348 | 0.124 | 0.167 | 0.152 | 0.205 |  0.372 |   1.648 |
| **S3Express**         |               |      |       |       |       |       |        |         |
|   —PutObject          |               | 1677 | 0.425 | 0.600 | 0.555 | 0.738 |  0.943 |  10.733 |
|   —GetObject          |               | 2842 | 0.207 | 0.274 | 0.256 | 0.305 |  0.422 |   9.291 |
| **Secrets Manager**   |               |      |       |       |       |       |        |         |
|   —GetSecretValue     |               | 1184 | 0.172 | 0.349 | 0.243 | 0.388 |  2.580 |   9.056 |
|   —PutSecretValue     |               |  421 | 0.267 | 0.567 | 0.533 | 0.636 |  0.820 |   9.148 |

## Methodology

This section describes how the benchmarks actually work at a high level:

### Selection criteria

These benchmarks select a handful of services to test against. The selection criterion is the top 7 services by traffic
coming from the AWS SDK for Kotlin (i.e., not from other SDKs, console, etc.). As of 7/28/2023, those top 7 services
were: S3, SNS, STS, CloudWatch, CloudWatch Events, DynamoDB, and Pinpoint (in descending order). However, Pinpoint has
strict throttling limits that make benchmarking impossible, so Secrets Manager is selected instead.

For each service, two APIs are selected roughly corresponding to a read and a write operation (e.g., S3::HeadObject is
a read operation and S3::PutObject is a write operation). Efforts are made to ensure that the APIs selected are the top
operations by traffic but alternate APIs may be selected in the case of low throttling limits, high setup complexity,
etc.

### Workflow

Benchmarks are run sequentially in a single thread. This is the high-level workflow for the benchmarks:

* For each benchmark service:
  * Instantiate a client with a [special telemetry provider](#telemetry-provider)
  * Run any necessary service-specific setup procedures (e.g., create/configure prerequisite resources)
  * For each benchmark operation:
    * Run any necessary operation-specific setup procedures (e.g., create/configure prerequisite resources)
    * Warmup the API call
    * Measure the API call
    * Aggregate operation metrics
    * Run any necessary operation-specific cleanup procedures (e.g., delete resources created in the setup step)
  * Run any necessary service-specific cleanup procedures (e.g., delete resources created in the setup step)
  * Print overall metrics summary

### Telemetry provider

A custom [benchmark-specific telemetry provider][1] is used to instrument each service client. This provider solely
handles metrics (i.e., no logging, tracing, etc.). It captures specific histogram metrics from an allowlist (currently
only `smithy.client.call.attempt_overhead_duration`) and aggregates them for the duration of an operation run (not
including the warmup phase). After the run is complete, the metrics are aggregated and various statistics are calculated
(e.g., minimum, average, median, etc.).

[1]: jvm/src/aws/sdk/kotlin/benchmarks/service/telemetry/BenchmarkTelemetryProvider.kt
