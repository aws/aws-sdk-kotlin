/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.collections.AttributeKey

// AWS-specific version of smithy-kotlin's SdkClientOption
public object AwsSdkClientOption {
    public val ApplicationId: AttributeKey<String> = AttributeKey("aws.sdk.kotlin#ApplicationId")
}
