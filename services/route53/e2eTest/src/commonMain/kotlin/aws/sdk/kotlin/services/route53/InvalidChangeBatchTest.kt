/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53

import aws.sdk.kotlin.services.route53.model.*
import aws.smithy.kotlin.runtime.io.use
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

// https://github.com/awslabs/aws-sdk-kotlin/issues/1433
class InvalidChangeBatchTest {
    @Test
    fun testMessageIsPopulated() = runTest {
        Route53Client {
            region = "us-east-1"
        }.use { client ->
            val createHostedZoneResp = client.createHostedZone {
                this.callerReference = (0..Int.MAX_VALUE).random().toString()
                this.name = "this-is-a-test-hosted-zone-for-aws-sdk-kotlin.com"
            }

            val hostedZoneId = checkNotNull(createHostedZoneResp.hostedZone?.id) { "Hosted zone is unexpectedly null" }

            try {
                val exception = assertFailsWith<InvalidChangeBatch> {
                    client.changeResourceRecordSets {
                        this.hostedZoneId = hostedZoneId
                        this.changeBatch = ChangeBatch {
                            this.changes = listOf(
                                Change {
                                    this.action = ChangeAction.Delete
                                    this.resourceRecordSet = ResourceRecordSet {
                                        this.name = "test.blerg.com"
                                        this.type = RrType.Cname
                                        this.ttl = 300
                                        this.resourceRecords = listOf(
                                            ResourceRecord {
                                                value = "test.blerg.com"
                                            },
                                        )
                                    }
                                },
                                Change {
                                    this.action = ChangeAction.Create
                                    this.resourceRecordSet = ResourceRecordSet {
                                        this.name = "test.blerg.com"
                                        this.type = RrType.Cname
                                        this.ttl = 300
                                        this.resourceRecords = listOf(
                                            ResourceRecord {
                                                value = "test.blerg.com"
                                            },
                                        )
                                    }
                                },
                            )
                            this.comment = "testing..."
                        }
                    }
                }

                assertNotNull(exception.message)
            } finally {
                client.deleteHostedZone {
                    id = hostedZoneId
                }
            }
        }
    }
}
