/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.runtime.ConfigurationException
import aws.smithy.kotlin.runtime.ErrorMetadata

/**
 * No credentials were available from this [CredentialsProvider]
 */
public class CredentialsNotLoadedException(message: String?, cause: Throwable? = null) : ClientException(message ?: "The provider could not provide credentials or required configuration was not set", cause)

/**
 * The [CredentialsProvider] was given an invalid configuration (e.g. invalid aws configuration file, invalid IMDS endpoint, etc)
 *
 * This is non-recoverable: it will not succeed on retry until the customer changes something.
 */
public class ProviderConfigurationException(message: String, cause: Throwable? = null) : ConfigurationException(message, cause) {
    init {
        sdkErrorMetadata.attributes[ErrorMetadata.NonRecoverable] = true
    }
}
