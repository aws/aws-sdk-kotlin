/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "S3 Transfer Manager for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: S3TransferManager"
extra["moduleName"] = "aws.sdk.kotlin.hll.s3transfermanager"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-http"))
                implementation(project(":services:s3")) // TODO: Hardcode an S3 Client version to avoid breakages
            }
        }
    }
}
