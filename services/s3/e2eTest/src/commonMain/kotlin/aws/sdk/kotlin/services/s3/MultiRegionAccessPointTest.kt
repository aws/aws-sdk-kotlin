/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.getAccountId
import aws.sdk.kotlin.e2etest.S3TestUtils.getBucketWithPrefix
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.putObject
import aws.sdk.kotlin.services.s3.withConfig
import aws.sdk.kotlin.services.s3control.S3ControlClient
import aws.sdk.kotlin.services.s3control.createMultiRegionAccessPoint
import aws.sdk.kotlin.services.s3control.deleteMultiRegionAccessPoint
import aws.sdk.kotlin.services.s3control.describeMultiRegionAccessPointOperation
import aws.sdk.kotlin.services.s3control.getMultiRegionAccessPoint
import aws.sdk.kotlin.services.s3control.model.Region
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.jvm.JvmStatic
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val MRAP_BUCKET_PREFIX = "s3-mrap-test-bucket-"
private const val MULTI_REGION_ACCESS_POINT_NAME = "aws-sdk-for-kotlin-test-multi-region-access-point"
private const val TEST_OBJECT_KEY = "test.txt"

class MutliRegionAccessPointTest {
    companion object {
        private lateinit var s3West: S3Client
        private lateinit var s3East: S3Client
        private lateinit var s3Control: S3ControlClient
        private lateinit var accountId: String
        private lateinit var multiRegionAccessPointArn: String
        private lateinit var usWestBucket: String
        private lateinit var usEastBucket: String

        @BeforeAll
        @JvmStatic
        fun setup() = runBlocking {
            s3West = S3Client { region = "us-west-2" }
            s3East = S3Client { region = "us-east-2" }
            s3Control = S3ControlClient { region = "us-west-2" }

            accountId = getAccountId()
            usWestBucket = getBucketWithPrefix(s3West, MRAP_BUCKET_PREFIX, "us-west-2", accountId)
            usEastBucket = getBucketWithPrefix(s3East, MRAP_BUCKET_PREFIX, "us-east-2", accountId)

            multiRegionAccessPointArn = s3Control.createMultiRegionAccessPoint(
                MULTI_REGION_ACCESS_POINT_NAME,
                accountId,
                listOf(usWestBucket, usEastBucket),
            )
        }

        @AfterAll
        @JvmStatic
        fun cleanup() = runBlocking {
            s3Control.deleteMultiRegionAccessPoint(MULTI_REGION_ACCESS_POINT_NAME, accountId)

            s3West.close()
            s3East.close()
            s3Control.close()
        }
    }

    @Test
    fun testMultiRegionAccessPointOperation() = runBlocking {
        // use .distinct() to deduplicate DefaultAwsSigner and CrtAwsSigner, which are the same on Native
        listOf(DefaultAwsSigner, CrtAwsSigner).distinct().forEach { signer ->
            testMultiRegionAccessPointOperation(signer)
        }
    }

    private suspend fun testMultiRegionAccessPointOperation(signer: AwsSigner) {
        println("Testing multi-region access point operations with $signer")

        val s3SigV4a = s3West.withConfig {
            authSchemes = listOf(SigV4AsymmetricAuthScheme(signer))
        }

        s3SigV4a.putObject {
            bucket = multiRegionAccessPointArn
            key = TEST_OBJECT_KEY
        }

        s3SigV4a.deleteObject {
            bucket = multiRegionAccessPointArn
            key = TEST_OBJECT_KEY
        }
    }
}

private suspend fun S3ControlClient.createMultiRegionAccessPoint(
    name: String,
    accountId: String,
    buckets: List<String>,
): String {
    println("Creating multi-region access point: $name")

    val requestTokenArn = checkNotNull(
        createMultiRegionAccessPoint {
            this.accountId = accountId
            details {
                this.name = name
                this.regions = buckets.map { Region { bucket = it } }
            }
        }.requestTokenArn,
    ) { "createMultiRegionAccessPoint requestTokenArn was unexpectedly null" }

    waitUntilOperationCompletes("createMultiRegionAccessPoint", accountId, requestTokenArn, 10.minutes)

    return getMultiRegionAccessPoint {
        this.name = name
        this.accountId = accountId
    }.accessPoint?.alias?.let {
        "arn:aws:s3::$accountId:accesspoint/$it"
    } ?: throw IllegalStateException("Failed to get ARN for multi-region access point $name")
}

private suspend fun S3ControlClient.deleteMultiRegionAccessPoint(
    name: String,
    accountId: String,
) {
    println("Deleting multi-region access point $name")

    val requestTokenArn = checkNotNull(
        deleteMultiRegionAccessPoint {
            this.accountId = accountId
            details {
                this.name = name
            }
        }.requestTokenArn,
    ) { "deleteMultiRegionAccessPoint requestTokenArn was unexpectedly null" }

    waitUntilOperationCompletes("deleteMultiRegionAccessPoint", accountId, requestTokenArn, 5.minutes)
}

private suspend fun S3ControlClient.waitUntilOperationCompletes(
    operation: String,
    accountId: String,
    requestTokenArn: String,
    timeout: Duration,
) = withTimeout(timeout) {
    var status: String? = null

    while (true) {
        val latestStatus = describeMultiRegionAccessPointOperation {
            this.accountId = accountId
            this.requestTokenArn = requestTokenArn
        }.asyncOperation?.requestStatus

        when (latestStatus) {
            "SUCCEEDED" -> {
                println("$operation operation succeeded.")
                return@withTimeout
            }
            "FAILED" -> throw IllegalStateException("$operation operation failed")
            else -> {
                if (status == null || latestStatus != status) {
                    println("Waiting for $operation to complete. Status: $latestStatus ")
                    status = latestStatus
                }
            }
        }

        delay(10.seconds)
    }
}
