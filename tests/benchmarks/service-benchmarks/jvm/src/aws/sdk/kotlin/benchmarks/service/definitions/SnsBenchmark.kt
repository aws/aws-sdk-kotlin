/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.sns.*

class SnsBenchmark : ServiceBenchmark<SnsClient> {
    private lateinit var arn: String

    override suspend fun client() = SnsClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: SnsClient) {
        arn = client.createTopic {
            name = Common.random("sdk-benchmark-topic-")
            attributes = mapOf("DisplayName" to "Foo")
        }.topicArn!!
    }

    override val operations get() = listOf(getTopicAttributesBenchmark, publishBenchmark)

    override suspend fun tearDown(client: SnsClient) {
        client.deleteTopic { topicArn = arn }
    }

    private val getTopicAttributesBenchmark = object : AbstractOperationBenchmark<SnsClient>("GetTopicAttributes") {
        override suspend fun transact(client: SnsClient) {
            client.getTopicAttributes {
                topicArn = arn
            }
        }
    }

    private val publishBenchmark = object : AbstractOperationBenchmark<SnsClient>("Publish") {
        override suspend fun transact(client: SnsClient) {
            client.publish {
                topicArn = arn
                message = Common.random()
            }
        }
    }
}
