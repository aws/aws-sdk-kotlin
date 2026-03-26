/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.utils

import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.withConfig
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.io.use

/**
 * An interceptor that emits the S3 Transfer Manager business metric
 */
internal object S3TransferManagerBusinessMetricInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
        context.executionContext.emitBusinessMetric(AwsBusinessMetric.S3_TRANSFER)
        return context.request
    }
}

internal inline fun <T> S3Client.withTmBusinessMetric(block: (S3Client) -> T): T = withConfig { interceptors += S3TransferManagerBusinessMetricInterceptor }.use(block)
