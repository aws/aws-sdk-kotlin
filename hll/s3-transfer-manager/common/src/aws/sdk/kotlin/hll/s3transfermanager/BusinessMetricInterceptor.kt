package aws.sdk.kotlin.hll.s3transfermanager

import aws.sdk.kotlin.runtime.http.interceptors.businessmetrics.AwsBusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor

/**
 * An interceptor that emits the S3 Transfer Manager business metric
 */
internal object BusinessMetricInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
        context.executionContext.emitBusinessMetric(AwsBusinessMetric.S3_TRANSFER)
        return context.request
    }
}