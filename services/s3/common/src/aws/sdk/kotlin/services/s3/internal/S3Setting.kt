/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.config.*

/**
 * S3 specific system settings
 */
internal object S3Setting {
    /**
     * Configure whether the S3 client should uses the access point ARN AWS region to construct the regional endpoint
     * for the request. See [Amazon S3 access points](https://docs.aws.amazon.com/sdkref/latest/guide/feature-s3-access-point.html)
     */
    public val UseArnRegion: EnvironmentSetting<Boolean> = boolEnvSetting("aws.s3UseArnRegion", "AWS_S3_USE_ARN_REGION")

    /**
     * Configure whether the S3 client potentially attempts cross-Region requests.
     * See [Amazon S3 Multi-Region Access Points](https://docs.aws.amazon.com/sdkref/latest/guide/feature-s3-mrap.html)
     */
    public val DisableMultiRegionAccessPoints: EnvironmentSetting<Boolean> = boolEnvSetting("aws.s3DisableMultiRegionAccessPoints", "AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS")

    /**
     * Configure whether requests made to S3 Express One Zone should use bucket-level session authentication or the default S3 authentication method.
     */
    public val DisableS3ExpressSessionAuth: MultipleKeyEnvironmentSetting<Boolean> = boolEnvSetting("aws.s3DisableExpressSessionAuth", "AWS_S3_DISABLE_EXPRESS_SESSION_AUTH")
        .withAdditionalKeys(listOf("aws.disableS3ExpressAuth"))
}
