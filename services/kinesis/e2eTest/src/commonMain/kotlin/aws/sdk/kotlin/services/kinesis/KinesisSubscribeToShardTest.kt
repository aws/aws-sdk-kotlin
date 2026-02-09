/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.kinesis

import aws.sdk.kotlin.services.kinesis.model.*
import aws.sdk.kotlin.services.kinesis.waiters.waitUntilStreamExists
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.retries.getOrThrow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

private val WAIT_TIMEOUT = 30.seconds
private val POLLING_RATE = 3.seconds

private val STREAM_NAME_PREFIX = "aws-sdk-kotlin-e2e-test-stream-"
private val STREAM_CONSUMER_NAME_PREFIX = "aws-sdk-kotlin-e2e-test-"

private val TEST_DATA = "Bees, bees, bees, bees!"

/**
 * Tests for Kinesis SubscribeToShard (an RPC-bound protocol)
 */
class KinesisSubscribeToShardTest {
    private val client = KinesisClient { region = "us-east-1" }

    private lateinit var dataStreamArn: String
    private lateinit var dataStreamConsumerArn: String

    @BeforeTest
    fun setup(): Unit = runBlocking {
        dataStreamArn = client.getOrCreateStream()
        dataStreamConsumerArn = client.getOrRegisterStreamConsumer()
    }

    @AfterTest
    fun cleanUp(): Unit = runBlocking {
        client.deregisterStreamConsumer {
            streamArn = dataStreamArn
            consumerArn = dataStreamConsumerArn
        }

        client.deleteStream {
            streamArn = dataStreamArn
        }
    }

    /**
     * Select the single shard ID associated with the data stream, and subscribe to it.
     * Read one event and make sure the data matches what's expected.
     */
    @Test
    fun testSubscribeToShard(): Unit = runBlocking {
        val dataStreamShardId = client.listShards {
            streamArn = dataStreamArn
        }.shards?.single()!!.shardId

        withAllEngines { engine ->
            client.withConfig {
                httpClient = engine
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
    }

    /**
     * Get a Kinesis data stream with the [STREAM_NAME_PREFIX], or if one does not exist,
     * create one and populate it with one test record.
     * @return the ARN of the data stream
     */
    private suspend fun KinesisClient.getOrCreateStream(): String = listStreams { }
        .streamSummaries
        ?.find { it.streamName?.startsWith(STREAM_NAME_PREFIX) ?: false }
        ?.streamArn ?: run {
        // Create a new data stream, then wait for it to be active
        val randomStreamName = STREAM_NAME_PREFIX + (0..Int.MAX_VALUE).random()
        createStream {
            streamName = randomStreamName
            shardCount = 1
        }

        val newStreamArn = waitUntilStreamExists({ streamName = randomStreamName })
            .getOrThrow()
            .streamDescription!!
            .streamArn!!

        // Put a record, then wait for it to appear on the stream
        putRecord {
            data = TEST_DATA.encodeToByteArray()
            streamArn = newStreamArn
            partitionKey = "Goodbye"
        }

        val newStreamShardId = client.listShards {
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
            }.records
                ?.firstOrNull { it.data?.decodeToString() == TEST_DATA }
        }

        newStreamArn
    }

    /**
     * Get a Kinesis data stream consumer, or if it doesn't exist, register a new one.
     * @return the ARN of the stream consumer
     */
    private suspend fun KinesisClient.getOrRegisterStreamConsumer(): String = listStreamConsumers { streamArn = dataStreamArn }
        .consumers
        ?.firstOrNull { it.consumerName?.startsWith(STREAM_CONSUMER_NAME_PREFIX) ?: false }
        ?.consumerArn ?: run {
        // Register a new consumer and wait for it to be active

        val randomConsumerName = STREAM_CONSUMER_NAME_PREFIX + (0..Int.MAX_VALUE).random()
        registerStreamConsumer {
            consumerName = randomConsumerName
            streamArn = dataStreamArn
        }

        waitForResource {
            listStreamConsumers { streamArn = dataStreamArn }
                ?.consumers
                ?.firstOrNull { it.consumerName == randomConsumerName }
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
    private suspend fun <T> KinesisClient.waitForResource(getResource: suspend () -> T?): T = withTimeout(WAIT_TIMEOUT) {
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
}
