/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.AwsServiceException
import software.aws.clientrt.http.HttpStatusCode
import software.aws.clientrt.http.operation.HttpDeserialize

/**
 * Metadata container about a modeled service exception
 *
 * @property errorCode The unique error code name that services use to identify errors
 * @property deserializer The deserializer responsible for providing a [Throwable] instance of the actual exception
 * @property httpStatusCode The HTTP status code the error is returned with
 */
public data class ExceptionMetadata(val errorCode: String, val deserializer: HttpDeserialize<*>, val httpStatusCode: HttpStatusCode? = null)

/**
 * Container for modeled exceptions
 */
public class ExceptionRegistry {
    // ErrorCode -> Meta
    private val errorsByCodeName = mutableMapOf<String, ExceptionMetadata>()

    /**
     * Register a modeled exception's metadata
     */
    public fun register(metadata: ExceptionMetadata) {
        errorsByCodeName[metadata.errorCode] = metadata
    }

    /**
     * Register a modeled service exception for the given [code]. The deserializer registered MUST provide
     * an [AwsServiceException] when invoked.
     */
    public fun register(code: String, deserializer: HttpDeserialize<*>, httpStatusCode: Int? = null) {
        register(ExceptionMetadata(code, deserializer, httpStatusCode?.let { HttpStatusCode.fromValue(it) }))
    }

    /**
     * Get the exception metadata associated with the given [code] name
     */
    public operator fun get(code: String?): ExceptionMetadata? = errorsByCodeName[code]
}
