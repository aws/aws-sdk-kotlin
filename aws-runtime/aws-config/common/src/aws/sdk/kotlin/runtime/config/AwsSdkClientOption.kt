/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Common client execution options.
 * AWS-specific version of [aws.smithy.kotlin.runtime.client.SdkClientOption]
 */
public object AwsSdkClientOption {
    /**
     * An optional application specific identifier.
     * When set it will be appended to the User-Agent header of every request in the form of: `app/{applicationId}`.
     */
    public val ApplicationId: AttributeKey<String> = AttributeKey("aws.sdk.kotlin#ApplicationId")
}
