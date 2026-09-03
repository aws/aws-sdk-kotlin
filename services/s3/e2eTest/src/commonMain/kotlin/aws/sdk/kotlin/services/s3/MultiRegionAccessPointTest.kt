/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.putObject
import aws.sdk.kotlin.services.s3.withConfig
import aws.sdk.kotlin.services.s3control.*
import aws.sdk.kotlin.services.s3control.model.Region
import aws.sdk.kotlin.services.s3control.paginators.listMultiRegionAccessPointsPaginated
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import aws.smithy.kotlin.runtime.testing.AfterAll
import aws.smithy.kotlin.runtime.testing.BeforeAll
import aws.smithy.kotlin.runtime.testing.TestInstance
import aws.smithy.kotlin.runtime.testing.TestLifecycle
import aws.smithy.kotlin.runtime.testing.parameterized
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TEST_OBJECT_KEY = "test.txt"

@TestInstance(TestLifecycle.PER_CLASS)
class MultiRegionAccessPointTest {
    private lateinit var s3West: S3Client
    private lateinit var s3East: S3Client
    private lateinit var s3Control: S3ControlClient

    private lateinit var accountId: String
    private lateinit var multiRegionAccessPointName: String
    private lateinit var multiRegionAccessPointArn: String
    private lateinit var usWestBucket: String
    private lateinit var usEastBucket: String

    @BeforeAll
    fun setup(): Unit = runBlocking {
        s3West = S3TestUtils.createClient { region = "us-west-2" }
        s3East = S3TestUtils.createClient { region = "us-east-2" }
        s3Control = S3ControlClient { region = "us-west-2" }

        accountId = S3TestUtils.getAccountId()
        usWestBucket = S3TestUtils.createTestBucket(s3West, "mrap-west")
        usEastBucket = S3TestUtils.createTestBucket(s3East, "mrap-east")

        multiRegionAccessPointName = "s3-test-mrap-${S3TestUtils.testRunId}"
        multiRegionAccessPointArn = s3Control.createMultiRegionAccessPoint(
            multiRegionAccessPointName,
            accountId,
            listOf(usWestBucket, usEastBucket),
        )
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        s3Control.deleteMultiRegionAccessPoint(multiRegionAccessPointName, accountId)

        val resp = s3Control.listMultiRegionAccessPointsPaginated {
            accountId = this@MultiRegionAccessPointTest.accountId
        }.toList().flatMap { it.accessPoints.orEmpty() }

        val mrapManifest = buildString {
            appendLine("Existing multi-region access points for account ID $accountId (${resp.size}):")
            resp.forEach { accessPoint ->
                appendLine("* ${accessPoint.name}:")
                appendLine("  * Alias: ${accessPoint.alias}")
                appendLine("  * Created: ${accessPoint.createdAt}")
                appendLine("  * Status: ${accessPoint.status}")

                val regions = accessPoint.regions.orEmpty()
                appendLine("  * Regions (${regions.size}):")

                regions.forEach { region ->
                    appendLine("    * ${region.region}: ${region.bucket} (account ID ${region.bucketAccountId})")
                }
            }
        }
        print(mrapManifest)

        S3TestUtils.deleteBucket(s3West, usWestBucket)
        S3TestUtils.deleteBucket(s3East, usEastBucket)

        s3West.close()
        s3East.close()
        s3Control.close()
    }

    @Test
    fun testMultiRegionAccessPointOperation(): Unit = parameterized(
        listOf(DefaultAwsSigner, CrtAwsSigner),
    ) { signer ->
        runBlocking {
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
}

/**
 * Create a multi-region access point named [name] in account [accountId] with [buckets] buckets.
 * @return the ARN of the multi-region access point that was created
 */
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

    return getMultiRegionAccessPointArn(name, accountId)
}

private suspend fun S3ControlClient.getMultiRegionAccessPointArn(
    name: String,
    accountId: String,
): String = getMultiRegionAccessPoint {
    this.name = name
    this.accountId = accountId
}.accessPoint?.alias?.let {
    "arn:aws:s3::$accountId:accesspoint/$it"
} ?: throw IllegalStateException("Failed to get ARN for multi-region access point $name")

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

/**
 * Continuously poll the status of [requestTokenArn] until its status is "SUCCEEDED" or [timeout] duration has passed.
 */
private suspend fun S3ControlClient.waitUntilOperationCompletes(
    operation: String,
    accountId: String,
    requestTokenArn: String,
    timeout: Duration,
) = withTimeout(timeout) {
    var status: String? = null

    while (true) {
        val response = describeMultiRegionAccessPointOperation {
            this.accountId = accountId
            this.requestTokenArn = requestTokenArn
        }
        when (val latestStatus = response.asyncOperation?.requestStatus) {
            "SUCCEEDED" -> {
                println("$operation operation succeeded.")
                return@withTimeout
            }
            "FAILED" -> {
                val code = response.asyncOperation?.responseDetails?.errorDetails?.code
                val message = response.asyncOperation?.responseDetails?.errorDetails?.message
                throw IllegalStateException("$operation operation failed. Code: $code. Message: $message")
            }
            else -> {
                if (status == null || latestStatus != status) {
                    println("Waiting for $operation to complete. Status: $latestStatus ")
                    status = latestStatus
                }
            }
        }

        delay(10.seconds) // Avoid constant status checks
    }
}
