/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.hll.configureKspCodegen
import org.gradle.kotlin.dsl.sourceSets

val sdkVersion: String by project
version = sdkVersion

description = "S3 Transfer Manager for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: S3 Transfer Manager"
extra["moduleName"] = "aws.sdk.kotlin.hll.s3transfermanager"

plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-http"))
                implementation(project(":services:s3"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.smithy.kotlin.http.test)
            }
        }
    }
}

configureKspCodegen(listOf(":hll:s3-transfer-manager-codegen"))
