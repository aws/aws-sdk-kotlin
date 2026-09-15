/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatch.getMetricData
import aws.sdk.kotlin.services.cloudwatch.model.MetricDataQuery
import aws.sdk.kotlin.services.cloudwatch.model.MetricDatum
import aws.sdk.kotlin.services.cloudwatch.putMetricData
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val NAMESPACE = "SdkBenchmark/testdata"
private const val METRIC_NAME = "foo"
private const val METRIC_VALUE = 42.0

class CloudwatchBenchmark : ServiceBenchmark<CloudWatchClient> {
    override suspend fun client() = CloudWatchClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override val operations get() = listOf(getMetricDataBenchmark, putMetricDataBenchmark)

    private val getMetricDataBenchmark = object : AbstractOperationBenchmark<CloudWatchClient>("GetMetricData") {
        // Default CloudWatch::GetMetricData max TPS is 50, so we artificially throttle the benchmark.
        // Adjust the run mode to ensure we get a minimum amount of transactions.
        override val warmupMode = RunMode.Iterations(500)
        override val iterationMode = RunMode.Iterations(1500)

        override suspend fun setup(client: CloudWatchClient) {
            client.putMetricData {
                namespace = NAMESPACE
                metricData = listOf(
                    MetricDatum {
                        metricName = METRIC_NAME
                        value = METRIC_VALUE
                        timestamp = Instant.now()
                    },
                )
            }
        }

        override suspend fun transact(client: CloudWatchClient) {
            delay(20.milliseconds) // Default CloudWatch::GetMetricData max TPS is 50
            client.getMetricData {
                startTime = Instant.now() - 5.minutes
                endTime = Instant.now()
                metricDataQueries = listOf(
                    MetricDataQuery {
                        id = "fooQuery"
                        metricStat {
                            metric {
                                namespace = NAMESPACE
                                metricName = METRIC_NAME
                            }
                            period = 60 // 60 seconds is the minimum period for regular resolution metrics
                            stat = "Average"
                        }
                        returnData = false // just return the stat, not the full dataset
                    },
                )
            }
        }
    }

    private val putMetricDataBenchmark = object : AbstractOperationBenchmark<CloudWatchClient>("PutMetricData") {
        override suspend fun transact(client: CloudWatchClient) {
            client.putMetricData {
                namespace = NAMESPACE
                metricData = listOf(
                    MetricDatum {
                        metricName = METRIC_NAME
                        value = METRIC_VALUE
                        timestamp = Instant.now()
                    },
                )
            }
        }
    }
}
