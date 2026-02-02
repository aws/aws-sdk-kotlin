/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.retries

import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.ServiceException
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType.Throttling
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType.Transient
import aws.smithy.kotlin.runtime.retries.policy.StandardRetryPolicy

/**
 * The standard policy for AWS service clients that defines which error conditions are retryable and how. This policy
 * will evaluate the following exceptions as retryable:
 *
 * * Any [ServiceException] with an `sdkErrorMetadata.errorCode` of:
 *   * `BandwidthLimitExceeded`
 *   * `EC2ThrottledException`
 *   * `IDPCommunicationError`
 *   * `LimitExceededException`
 *   * `PriorRequestNotComplete`
 *   * `ProvisionedThroughputExceededException`
 *   * `RequestLimitExceeded`
 *   * `RequestThrottled`
 *   * `RequestThrottledException`
 *   * `RequestTimeout`
 *   * `RequestTimeoutException`
 *   * `SlowDown`
 *   * `ThrottledException`
 *   * `Throttling`
 *   * `ThrottlingException`
 *   * `TooManyRequestsException`
 *   * `TransactionInProgressException`
 * * Any [ServiceException] with an `sdkErrorMetadata.statusCode` of:
 *   * 500 (Internal Service Error)
 *   * 502 (Bad Gateway)
 *   * 503 (Service Unavailable)
 *   * 504 (Gateway Timeout)
 *
 * If none of those conditions match, this policy delegates to [StandardRetryPolicy]. See that class's documentation for
 * more information about how it evaluates exceptions.
 */
public open class AwsRetryPolicy : StandardRetryPolicy() {
    public companion object {
        /**
         * The default [aws.smithy.kotlin.runtime.retries.policy.RetryPolicy] used by AWS service clients
         */
        public val Default: AwsRetryPolicy = AwsRetryPolicy()

        internal val knownErrorTypes = mapOf(
            "BandwidthLimitExceeded" to Throttling,
            "EC2ThrottledException" to Throttling,
            "IDPCommunicationError" to Transient,
            "LimitExceededException" to Throttling,
            "PriorRequestNotComplete" to Throttling,
            "ProvisionedThroughputExceededException" to Throttling,
            "RequestLimitExceeded" to Throttling,
            "RequestThrottled" to Throttling,
            "RequestThrottledException" to Throttling,
            "RequestTimeout" to Transient,
            "RequestTimeoutException" to Transient,
            "SlowDown" to Throttling,
            "ThrottledException" to Throttling,
            "Throttling" to Throttling,
            "ThrottlingException" to Throttling,
            "TooManyRequestsException" to Throttling,
            "TransactionInProgressException" to Throttling,
        )

        internal val knownStatusCodes = mapOf(
            500 to Transient,
            502 to Transient,
            503 to Transient,
            504 to Transient,
        )
    }

    override fun evaluateSpecificExceptions(ex: Throwable): RetryDirective? = when (ex) {
        is ServiceException -> evaluateServiceException(ex)
        else -> null
    }

    private fun evaluateServiceException(ex: ServiceException): RetryDirective? = with(ex.sdkErrorMetadata) {
        (knownErrorTypes[errorCode] ?: knownStatusCodes[statusCode])
            ?.let { RetryDirective.RetryError(it) }
    }

    private val ServiceErrorMetadata.statusCode: Int?
        get() = (protocolResponse as? HttpResponse)?.status?.value
}
