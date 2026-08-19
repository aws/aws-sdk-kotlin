/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.cloudwatchevents.*
import aws.sdk.kotlin.services.cloudwatchevents.model.PutEventsRequestEntry
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class CloudwatchEventsBenchmark : ServiceBenchmark<CloudWatchEventsClient> {
    private lateinit var eventBus: String

    override suspend fun client() = CloudWatchEventsClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: CloudWatchEventsClient) {
        eventBus = Common.random("sdk-benchmark-eventbus-")
        client.createEventBus {
            name = eventBus
        }
    }

    override val operations get() = listOf(describeEventBusBenchmark, putEventsBenchmark)

    override suspend fun tearDown(client: CloudWatchEventsClient) {
        client.deleteEventBus {
            name = eventBus
        }
    }

    private val describeEventBusBenchmark =
        object : AbstractOperationBenchmark<CloudWatchEventsClient>("DescribeEventBus") {
            // Default CloudWatchEvents::DescribeEventBus max TPS is 50, so we artificially throttle the benchmark.
            // Adjust the run mode to ensure we get a minimum amount of transactions.
            override val warmupMode = RunMode.Iterations(500)
            override val iterationMode = RunMode.Iterations(1500)

            override suspend fun transact(client: CloudWatchEventsClient) {
                delay(20.milliseconds) // Default CloudWatchEvents::DescribeEventBus max TPS is 50
                client.describeEventBus {
                    name = eventBus
                }
            }
        }

    private val putEventsBenchmark =
        object : AbstractOperationBenchmark<CloudWatchEventsClient>("PutEvents") {
            override suspend fun transact(client: CloudWatchEventsClient) {
                client.putEvents {
                    entries = listOf(
                        PutEventsRequestEntry {
                            eventBusName = eventBus
                            detail = """{ "foo": "bar" }"""
                            detailType = "foo"
                            source = "baz"
                            time = Instant.now()
                        },
                    )
                }
            }
        }
}
