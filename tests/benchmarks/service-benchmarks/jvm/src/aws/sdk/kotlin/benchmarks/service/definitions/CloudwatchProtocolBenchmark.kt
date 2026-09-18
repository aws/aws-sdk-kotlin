/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatch.getMetricData
import aws.sdk.kotlin.services.cloudwatch.listMetrics
import aws.sdk.kotlin.services.cloudwatch.model.Dimension
import aws.sdk.kotlin.services.cloudwatch.model.Metric
import aws.sdk.kotlin.services.cloudwatch.model.MetricDataQuery
import aws.sdk.kotlin.services.cloudwatch.model.MetricDatum
import aws.sdk.kotlin.services.cloudwatch.putMetricData
import aws.smithy.kotlin.runtime.time.Instant
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class CloudwatchProtocolBenchmark : ServiceProtocolBenchmark<CloudWatchClient> {
    companion object {
        val suiteId = UUID.randomUUID()
        val baseTime = Instant.now() - 2.hours
        const val TEST_NAME_SPACE = "SDK Benchmark Test Data"
    }

    override suspend fun client() = CloudWatchClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
            connectTimeout = 30.seconds
        }
    }

    override val scales = listOf(16, 64, 256, 1000)

    override val operations get() = listOf(
        putMetricDataBenchmark,
        getMetricDataBenchmark,
        listMetricsBenchmark,
    )

    private val putMetricDataBenchmark = object : AbstractOperationProtocolBenchmark<CloudWatchClient>("Put metric data") {
        override val requireScaling = true

        override suspend fun transact(client: CloudWatchClient, scale: Int, iteration: Int) {
            client.putMetricData {
                namespace = TEST_NAME_SPACE
                metricData = (0 until scale).map { metricDatumIndex ->
                    MetricDatum {
                        metricName = "TestMetric"
                        dimensions = listOf(
                            Dimension {
                                name = "TestDimension"
                                value = "$suiteId-$scale"
                            },
                        )
                        value = Random.nextDouble()
                        unit = null
                        timestamp = baseTime + ((metricDatumIndex + 1) * 2).seconds
                    }
                }.toList()
            }
        }
    }

    private val getMetricDataBenchmark = object : AbstractOperationProtocolBenchmark<CloudWatchClient>("Get metric data") {
        override val requireScaling = true

        override suspend fun transact(client: CloudWatchClient, scale: Int, iteration: Int) {
            client.getMetricData {
                startTime = baseTime
                endTime = baseTime + 2.hours
                metricDataQueries = listOf(
                    MetricDataQuery {
                        id = "m0"
                        returnData = true
                        metricStat {
                            unit = null
                            stat = "Sum"
                            metric = Metric {
                                namespace = TEST_NAME_SPACE
                                metricName = "TestMetric"
                                dimensions = listOf(
                                    Dimension {
                                        name = "TestDimension"
                                        value = "$suiteId-$scale"
                                    },
                                )
                            }
                            period = 60
                        }
                    },
                )
            }
        }
    }

    private val listMetricsBenchmark = object : AbstractOperationProtocolBenchmark<CloudWatchClient>("List metrics") {
        override val requireScaling = true

        override suspend fun transact(client: CloudWatchClient, scale: Int, iteration: Int) {
            client.listMetrics {
                namespace = TEST_NAME_SPACE
            }
        }
    }
}
