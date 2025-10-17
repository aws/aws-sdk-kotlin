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
                implementation(libs.s3)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.smithy.kotlin.test.jvm)
                implementation(libs.smithy.kotlin.testing.jvm)
            }
        }
    }
}
