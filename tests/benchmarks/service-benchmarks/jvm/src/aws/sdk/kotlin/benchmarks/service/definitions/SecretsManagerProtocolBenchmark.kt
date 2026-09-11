/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.benchmarks.service.DEFAULT_ITERATIONS
import aws.sdk.kotlin.services.secretsmanager.*
import aws.sdk.kotlin.services.secretsmanager.model.Filter
import aws.sdk.kotlin.services.secretsmanager.model.FilterNameStringType
import aws.sdk.kotlin.services.secretsmanager.model.Tag

class SecretsManagerProtocolBenchmark : ServiceProtocolBenchmark<SecretsManagerClient> {
    companion object {
        private val runStartTimestamp = System.currentTimeMillis() / 1000L
        private inline fun Int.padded(): String = String.format("%03d", this)
    }

    override suspend fun client() = SecretsManagerClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: SecretsManagerClient) {
        // FIXME: Add metadata parameters to setup/teardown methods to pass configuration info
        // (e.g., configured iteration count). Larger refactoring needed outside this PR scope.

        for (iteration in 1..DEFAULT_ITERATIONS) {
            val tags = listOf(
                Tag {
                    key = "Stage"
                    value = "Production"
                },
                Tag {
                    key = "Iteration"
                    value = "${iteration.padded()}"
                },
            )

            client.createSecret {
                name = "TestSecret_${runStartTimestamp}_${iteration.padded()}"
                secretString = "A temporary secret value"
                description = "The testing secret for run ${iteration.padded()}"
                this.tags = tags
            }

            client.createSecret {
                name = "TestBinarySecret_${runStartTimestamp}_${iteration.padded()}"
                secretBinary = "A temporary secret value".toByteArray()
                description = "The binary testing secret for run ${iteration.padded()}"
                this.tags = tags
            }
        }
    }

    override suspend fun tearDown(client: SecretsManagerClient) {
        for (iteration in 1..DEFAULT_ITERATIONS) {
            client.deleteSecret {
                secretId = "TestSecret_${runStartTimestamp}_${iteration.padded()}"
                forceDeleteWithoutRecovery = true
            }
            client.deleteSecret {
                secretId = "TestBinarySecret_${runStartTimestamp}_${iteration.padded()}"
                forceDeleteWithoutRecovery = false
            }
        }
    }

    override val operations get() = listOf(
        putStringSecretBenchmark,
        putBinarySecretBenchmark,
        getStringSecretBenchmark,
        getBinarySecretBenchmark,
        describeSecretBenchmark,
        listSecretsBenchmark,
    )

    override val scales = listOf(64, 512, 4096, 8192, 45056)

    private val putStringSecretBenchmark = object : AbstractOperationProtocolBenchmark<SecretsManagerClient>("Put string secret") {
        override val requireScaling = true

        override suspend fun transact(client: SecretsManagerClient, scale: Int, iteration: Int) {
            val secretId = "TestSecret_${runStartTimestamp}_${iteration.padded()}"

            val secretString = Common.randomStringPayload(scale)
            client.putSecretValue {
                this.secretId = secretId
                this.secretString = secretString
            }
        }
    }

    private val putBinarySecretBenchmark = object : AbstractOperationProtocolBenchmark<SecretsManagerClient>("Put binary secret") {
        override val requireScaling = true

        override suspend fun transact(client: SecretsManagerClient, scale: Int, iteration: Int) {
            val secretId = "TestBinarySecret_${runStartTimestamp}_${iteration.padded()}"

            val secretBinary = Common.randomBytesPayload(scale)
            client.putSecretValue {
                this.secretId = secretId
                this.secretBinary = secretBinary
            }
        }
    }

    private val getStringSecretBenchmark = object : AbstractOperationProtocolBenchmark<SecretsManagerClient>("Get string secret") {
        override val requireScaling = true

        override suspend fun transact(client: SecretsManagerClient, scale: Int, iteration: Int) {
            val secretId = "TestSecret_${runStartTimestamp}_${iteration.padded()}"

            client.getSecretValue {
                this.secretId = secretId
            }
        }
    }

    private val getBinarySecretBenchmark = object : AbstractOperationProtocolBenchmark<SecretsManagerClient>("Get binary secret") {
        override val requireScaling = true

        override suspend fun transact(client: SecretsManagerClient, scale: Int, iteration: Int) {
            val secretId = "TestBinarySecret_${runStartTimestamp}_${iteration.padded()}"

            client.getSecretValue {
                this.secretId = secretId
            }
        }
    }

    private val describeSecretBenchmark = object : AbstractOperationProtocolBenchmark<SecretsManagerClient>("Describe secret") {
        override val requireScaling = false

        override suspend fun transact(client: SecretsManagerClient, scale: Int, iteration: Int) {
            val secretId = "TestSecret_${runStartTimestamp}_${iteration.padded()}"

            client.describeSecret {
                this.secretId = secretId
            }
        }
    }

    private val listSecretsBenchmark = object : AbstractOperationProtocolBenchmark<SecretsManagerClient>("List secrets") {
        override val requireScaling = false

        override suspend fun transact(client: SecretsManagerClient, scale: Int, iteration: Int) {
            val filters = listOf(
                Filter {
                    key = FilterNameStringType.fromValue("tag-key")
                    values = listOf("Iteration")
                },
                Filter {
                    key = FilterNameStringType.fromValue("tag-value")
                    values = listOf("${iteration.padded()}")
                },
            )

            client.listSecrets {
                this.filters = filters
            }
        }
    }
}
