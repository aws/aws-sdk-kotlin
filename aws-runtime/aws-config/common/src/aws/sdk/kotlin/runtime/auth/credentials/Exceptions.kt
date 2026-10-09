/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.runtime.ConfigurationException

/**
 * No credentials were available from this [CredentialsProvider]
 */
public class CredentialsNotLoadedException(message: String?, cause: Throwable? = null) : ClientException(message ?: "The provider could not provide credentials or required configuration was not set", cause)

/**
 * The [CredentialsProvider] was given an invalid configuration (e.g. invalid aws configuration file, invalid IMDS endpoint, etc)
 */
public class ProviderConfigurationException(message: String, cause: Throwable? = null) : ConfigurationException(message, cause)
