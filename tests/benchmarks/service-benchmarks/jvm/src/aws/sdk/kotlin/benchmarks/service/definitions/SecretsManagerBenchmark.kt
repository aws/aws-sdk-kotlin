/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.secretsmanager.*

class SecretsManagerBenchmark : ServiceBenchmark<SecretsManagerClient> {
    private val secretName = Common.random("sdk-benchmark-secret-name-")

    override suspend fun client() = SecretsManagerClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: SecretsManagerClient) {
        client.createSecret {
            name = secretName
            description = "A dummy secret used for benchmarking Secrets Manager"
        }
    }

    override suspend fun tearDown(client: SecretsManagerClient) {
        client.deleteSecret { this.secretId = secretName }
    }

    override val operations get() = listOf(getSecretValueBenchmark, putSecretValueBenchmark)

    private val getSecretValueBenchmark = object : AbstractOperationBenchmark<SecretsManagerClient>("GetSecretValue") {
        override suspend fun setup(client: SecretsManagerClient) {
            client.putSecretValue {
                secretId = secretName
                secretString = "secret"
            }
        }

        override suspend fun transact(client: SecretsManagerClient) {
            client.getSecretValue {
                secretId = secretName
            }
        }
    }

    private val putSecretValueBenchmark = object : AbstractOperationBenchmark<SecretsManagerClient>("PutSecretValue") {
        override suspend fun transact(client: SecretsManagerClient) {
            client.putSecretValue {
                secretId = secretName
                secretString = "secret"
            }
        }
    }
}
