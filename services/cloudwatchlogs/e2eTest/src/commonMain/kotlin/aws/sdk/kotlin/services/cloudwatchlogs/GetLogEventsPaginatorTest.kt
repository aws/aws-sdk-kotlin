/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.services.cloudwatchlogs.*
import aws.sdk.kotlin.services.cloudwatchlogs.model.GetLogEventsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.OutputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.paginators.getLogEventsPaginated
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.util.Uuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val MESSAGES_PER_BATCH = 100
private const val BATCHES = 10
private const val TOTAL_MESSAGES = MESSAGES_PER_BATCH * BATCHES
private val TIMESTAMP_DELTA_PER_MESSAGE = (-1).milliseconds
private val POLLING_DELAY = 5.seconds

// Uncertain what a reliable value for this is...technically, _any_ amount of empty pages is possible given how
// token-based pagination works. 🤷
private const val MAX_SEQUENTIAL_EMPTY_PAGES = 5

class GetLogEventsPaginatorTest {
    @Test
    fun testGetLogEventsPagination() = runTest {
        val cwl = CloudWatchLogsClient.fromEnvironment()
        try {
            val (group, stream) = cwl.createLogGroupStream()

            try {
                cwl.publishMessageBatches(group, stream)

                val eventFlow = cwl.getLogEventsPaginated {
                    logGroupName = group
                    logStreamName = stream
                    limit = MESSAGES_PER_BATCH
                    startFromHead = true
                }

                assertFlowTerminates(eventFlow)
            } finally {
                cwl.deleteLogGroupStream(group, stream)
            }
        } finally {
            cwl.close()
        }
    }
}

private suspend fun assertFlowTerminates(eventFlow: Flow<GetLogEventsResponse>) {
    var maxSeen = 0
    var sequentialEmptyPages = 0
    var totalPages = 0

    eventFlow.collect { page ->
        totalPages++
        println("Page $totalPages:")
        println("  + token: ${page.nextForwardToken}")
        println("  + items: ${page.events.orEmpty().size}")
        println()

        val events = page.events.orEmpty()
        if (events.isEmpty()) {
            assertTrue(
                """
                    Too many sequential empty pages ($MAX_SEQUENTIAL_EMPTY_PAGES). It's likely the log event flow is not
                    terminating properly.
                    * Max message index seen: $maxSeen
                    * Total pages seen: $totalPages
                """.trimIndent(),
            ) { sequentialEmptyPages++ < MAX_SEQUENTIAL_EMPTY_PAGES }
        } else {
            sequentialEmptyPages = 0

            val batchMaxIndex = events.maxOf { it.messageIndex }
            assertTrue(
                """
                    Unexpected repetition of a log event. Current batch contains message index $batchMaxIndex but already
                    encountered message index $maxSeen on a prior page.
                    * Total pages seen: $totalPages
                """.trimIndent(),
            ) { batchMaxIndex >= maxSeen }

            maxSeen = batchMaxIndex
        }
    }

    assertTrue(
        """
            Not enough pages seen. Expected to see at least $BATCHES but only saw up $totalPages.
            * Max message index seen: $maxSeen
        """.trimIndent(),
    ) { totalPages >= BATCHES }

    assertTrue(
        """
            Saw an unexpected maximum message index. Expected to see exactly $TOTAL_MESSAGES but saw $maxSeen instead.
            * Total pages seen: $totalPages
        """.trimIndent(),
    ) { maxSeen == TOTAL_MESSAGES }
}

private fun createMessageBatches(anchorTime: Instant) = (0 until BATCHES).map { batchIndex ->
    (0 until MESSAGES_PER_BATCH).map { batchMessageIndex ->
        val overallMessageIndex = batchIndex * MESSAGES_PER_BATCH + batchMessageIndex
        val timestampDelta = TIMESTAMP_DELTA_PER_MESSAGE * (TOTAL_MESSAGES - overallMessageIndex)

        InputLogEvent {
            message = "Message ${overallMessageIndex + 1}/$TOTAL_MESSAGES (${batchMessageIndex + 1}/$MESSAGES_PER_BATCH in batch ${batchIndex + 1}/$BATCHES)"

            timestamp = (anchorTime + timestampDelta).epochMilliseconds
        }
    }
}

private suspend fun CloudWatchLogsClient.createLogGroupStream(): Pair<String, String> {
    val group = "paginator-test-group_${Uuid.random()}"
    val stream = "paginator-test-stream_${Uuid.random()}"

    createLogGroup { logGroupName = group }
    println("Created log group $group")

    try {
        createLogStream {
            logGroupName = group
            logStreamName = stream
        }
    } catch (e: Throwable) {
        deleteLogGroup { logGroupName = group }
        throw e
    }
    println("Created log stream $stream")

    return group to stream
}

private suspend fun CloudWatchLogsClient.deleteLogGroupStream(group: String, stream: String) {
    deleteLogStream {
        logGroupName = group
        logStreamName = stream
    }
    println("Deleted log stream $stream")

    deleteLogGroup { logGroupName = group }
    println("Deleted log group $group")
}

private suspend fun CloudWatchLogsClient.publishMessageBatches(group: String, stream: String) = coroutineScope {
    val messageBatches = createMessageBatches(Instant.now())

    println()
    messageBatches.mapIndexed { index, batch ->
        async {
            putLogEvents {
                logGroupName = group
                logStreamName = stream
                logEvents = batch
            }
            println("Published message batch ${index + 1} consisting of ${batch.size} events")
        }
    }.awaitAll()
    println()

    println("Delaying for $POLLING_DELAY to allow for eventual consistency...")
    delay(POLLING_DELAY)
}

private val OutputLogEvent.messageIndex: Int
    // Index is first number in message
    get() = requireNotNull(message)
        .dropWhile { !it.isDigit() }
        .takeWhile { it.isDigit() }
        .toInt()
