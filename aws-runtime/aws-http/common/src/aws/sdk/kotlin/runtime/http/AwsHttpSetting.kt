/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.config.EnvironmentSetting
import aws.smithy.kotlin.runtime.config.boolEnvSetting

/**
 * Settings resolved from environment variables and system properties for the AWS HTTP runtime.
 */
@InternalSdkApi
public object AwsHttpSetting {
    /**
     * Enables the new retry behavior. When set, takes precedence over
     * the `SMITHY_NEW_RETRIES_2026` environment variable defined in smithy-kotlin.
     */
    public val AwsNewRetries: EnvironmentSetting<Boolean> = boolEnvSetting("aws.newRetries2026", "AWS_NEW_RETRIES_2026")
}
