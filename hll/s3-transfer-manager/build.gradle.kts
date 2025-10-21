/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.sourceSets

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "S3 Transfer Manager for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: S3 Transfer Manager"
extra["moduleName"] = "aws.sdk.kotlin.hll.s3transfermanager"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-http"))
                implementation(project(":services:s3"))
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
