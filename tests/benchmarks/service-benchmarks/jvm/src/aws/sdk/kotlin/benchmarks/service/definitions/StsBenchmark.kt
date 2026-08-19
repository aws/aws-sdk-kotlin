/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.iam.IamClient
import aws.sdk.kotlin.services.iam.createRole
import aws.sdk.kotlin.services.iam.deleteRole
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.assumeRole
import aws.sdk.kotlin.services.sts.model.StsException
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.telemetry.TelemetryProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class StsBenchmark : ServiceBenchmark<StsClient> {
    override suspend fun client() = StsClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override val operations get() = listOf(assumeRoleBenchmark, getCallerIdentityBenchmark)

    private val assumeRoleBenchmark = object : AbstractOperationBenchmark<StsClient>("AssumeRole") {
        private lateinit var iamRoleArn: String
        private lateinit var iamRoleName: String

        override suspend fun setup(client: StsClient) {
            iamRoleName = Common.random("sdk-benchmark-role-")
            val callerArn = client.getCallerIdentity().arn!!

            iam {
                val resp = createRole {
                    roleName = iamRoleName
                    assumeRolePolicyDocument = assumeRolePolicyJson(callerArn)
                }

                iamRoleArn = resp.role!!.arn!!
            }

            // It takes a while for newly-created roles to fully propagate to STS. In the meantime, trying to assume the
            // role causes an exception. Example:
            // | StsException: User: arn:aws:iam::123456789012:user/Username is not authorized to perform:
            // | sts:AssumeRole on resource: arn:aws:iam::123456789012:role/RoleName
            // Even after a single STS::AssumeRole success, subsequent calls _may still be unsuccessful_. Thus, we ping
            // every second until we get 3 successful calls –or– an unexpected error is thrown.
            withTimeout(30.seconds) {
                var successes = 0
                while (successes < 3) {
                    try {
                        delay(1.seconds)
                        client.assumeRole {
                            roleArn = iamRoleArn
                            roleSessionName = Common.random("sdk-benchmark-session-")
                        }
                    } catch (e: StsException) {
                        if (e.isNotAuthorized) {
                            // Role still being propagated to STS
                            continue
                        } else {
                            // Some other error we didn't expect, throw hands up
                            throw e
                        }
                    }

                    // STS successfully assumed role, chalk it up in the win column.
                    successes++
                }

                // Saw enough successes, we're good to go.
            }
        }

        override suspend fun transact(client: StsClient) {
            client.assumeRole {
                roleArn = iamRoleArn
                roleSessionName = Common.random("sdk-benchmark-session-")
            }
        }

        override suspend fun tearDown(client: StsClient) {
            iam { deleteRole { roleName = iamRoleName } }
        }
    }

    private val getCallerIdentityBenchmark = object : AbstractOperationBenchmark<StsClient>("GetCallerIdentity") {
        override suspend fun transact(client: StsClient) {
            client.getCallerIdentity()
        }
    }
}

private suspend inline fun <T> iam(block: IamClient.() -> T) = IamClient
    .fromEnvironment { telemetryProvider = TelemetryProvider.None }
    .use(block)

private val StsException.isNotAuthorized: Boolean
    get() = message?.contains("is not authorized to perform") == true

private fun assumeRolePolicyJson(principalArn: String) = // language=JSON
    """
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {
                        "AWS": [ "$principalArn" ]
                    },
                    "Action": [
                        "sts:AssumeRole"
                    ]
                }
            ]
        }
    """.trimIndent()
