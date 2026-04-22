/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.kinesis

import aws.sdk.kotlin.services.kinesis.model.*
import aws.sdk.kotlin.services.kinesis.waiters.waitUntilStreamExists
import aws.sdk.kotlin.services.kinesis.waiters.waitUntilStreamNotExists
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.retries.getOrThrow
import aws.smithy.kotlin.runtime.testing.TestInstance
import aws.smithy.kotlin.runtime.testing.TestLifecycle
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private val WAIT_TIMEOUT = 30.seconds
private val POLLING_RATE = 3.seconds

private val STREAM_NAME_PREFIX = "aws-sdk-kotlin-e2e-test-stream"
private val STREAM_CONSUMER_NAME_PREFIX = "aws-sdk-kotlin-e2e-test"

private val TEST_DATA = "Bees, bees, bees, bees!"

/**
 * Tests for Kinesis SubscribeToShard (an RPC-bound protocol)
 */
@TestInstance(TestLifecycle.PER_CLASS)
class KinesisSubscribeToShardTest {
    private val client = KinesisClient { region = "us-east-1" }

    private val testRunId = Instant
        .now()
        .format(TimestampFormat.ISO_8601_CONDENSED)
        .lowercase()

    private val testStreamName = "$STREAM_NAME_PREFIX-$testRunId"
    private val testConsumerName = "$STREAM_CONSUMER_NAME_PREFIX-$testRunId"

    /**
     * Select the single shard ID associated with the data stream, and subscribe to it.
     * Read one event and make sure the data matches what's expected.
     */
    @Test
    fun testSubscribeToShard(): Unit = runBlocking {
        val dataStreamArn = client.createStream(testStreamName)

        try {
            val dataStreamConsumerArn = client.registerStreamConsumerAndWait(testConsumerName, dataStreamArn)

            try {
                val dataStreamShardId = client.listShards {
                    streamArn = dataStreamArn
                }.shards?.single()!!.shardId

                withAllEngines { context ->
                    client.withConfig {
                        httpClient = context.engine
                    }.use { clientWithTestEngine ->
                        clientWithTestEngine.subscribeToShard(
                            SubscribeToShardRequest {
                                consumerArn = dataStreamConsumerArn
                                shardId = dataStreamShardId
                                startingPosition = StartingPosition {
                                    type = ShardIteratorType.TrimHorizon
                                }
                            },
                        ) {
                            val event = it.eventStream?.first()
                            val record = event?.asSubscribeToShardEvent()?.records?.single()
                            assertEquals(TEST_DATA, record?.data?.decodeToString())
                        }

                        // Wait 5 seconds, otherwise a ResourceInUseException gets thrown. Source:
                        // https://docs.aws.amazon.com/kinesis/latest/APIReference/API_SubscribeToShard.html
                        // > If you call SubscribeToShard 5 seconds or more after a successful call, the second call takes over the subscription
                        delay(5.seconds)
                    }
                }
            } finally {
                client.deregisterStreamConsumer {
                    streamArn = dataStreamArn
                    consumerArn = dataStreamConsumerArn
                }
            }
        } finally {
            client.deleteStream { streamArn = dataStreamArn }
            client.waitUntilStreamNotExists { streamArn = dataStreamArn }
        }
    }
}

/**
 * Creates a Kinesis data stream with the prefix [STREAM_NAME_PREFIX] prefix and populate it with one test record.
 * @param name The name to use for the new stream
 * @return the ARN of the data stream
 */
private suspend fun KinesisClient.createStream(name: String): String {
    // Create a new data stream, then wait for it to be active
    createStream {
        streamName = name
        shardCount = 1
    }

    val newStreamArn = waitUntilStreamExists { streamName = name }
        .getOrThrow()
        .streamDescription!!
        .streamArn

    // Put a record, then wait for it to appear on the stream
    putRecord {
        data = TEST_DATA.encodeToByteArray()
        streamArn = newStreamArn
        partitionKey = "Goodbye"
    }

    val newStreamShardId = listShards {
        streamArn = newStreamArn
    }.shards?.single()!!.shardId

    val currentShardIterator = getShardIterator {
        shardId = newStreamShardId
        shardIteratorType = ShardIteratorType.TrimHorizon
        streamArn = newStreamArn
    }.shardIterator!!

    waitForResource {
        getRecords {
            shardIterator = currentShardIterator
            streamArn = newStreamArn
        }.records.firstOrNull { it.data.decodeToString() == TEST_DATA }
    }

    return newStreamArn
}

/**
 * Register a new consumer and wait for it to be active
 * @param name The name to use for the new consumer
 * @param dataStreamArn The ARN of the data stream to consume
 * @return the ARN of the stream consumer
 */
private suspend fun KinesisClient.registerStreamConsumerAndWait(name: String, dataStreamArn: String): String {
    registerStreamConsumer {
        consumerName = name
        streamArn = dataStreamArn
    }

    return waitForResource {
        listStreamConsumers { streamArn = dataStreamArn }
            .consumers
            ?.firstOrNull { it.consumerName == name }
            ?.takeIf { it.consumerStatus == ConsumerStatus.Active }
            ?.consumerArn
    }
}

/**
 * Poll at a predefined [POLLING_RATE] for a resource to exist and return it.
 * Throws an exception if this takes longer than the [WAIT_TIMEOUT] duration.
 *
 * @param getResource a suspending function which returns the resource or null if it does not exist yet
 * @return the resource
 */
private suspend fun <T> waitForResource(getResource: suspend () -> T?): T = withTimeout(WAIT_TIMEOUT) {
    var resource: T? = null
    while (resource == null) {
        resource = getResource()
        resource ?: run {
            delay(POLLING_RATE)
            yield()
        }
    }
    return@withTimeout resource
}
