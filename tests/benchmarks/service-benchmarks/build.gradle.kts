/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.skipPublishing

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp)
}

skipPublishing()

val requiredServices = setOf(
    // keep this list in sync with <rootdir>/settings.gradle.kts

    // Top 6 services called by Kotlin SDK customers as of 7/25/2023, in descending order of call volume,
    // plus Secrets Manager which replaced Pinpoint after new API throttling limits broke our benchmark.
    "s3",
    "sns",
    "sts",
    "cloudwatch",
    "cloudwatchevents",
    "dynamodb",
    "secretsmanager",

    // Services required as prerequisites for setup
    "iam", // Create roles for STS::AssumeRole
)

val optinAnnotations = listOf("kotlin.RequiresOptIn", "aws.smithy.kotlin.runtime.InternalApi")

kotlin {
    sourceSets {
        all {
            optinAnnotations.forEach { languageSettings.optIn(it) }
        }

        jvmMain {
            dependencies {
                api(libs.smithy.kotlin.runtime.core)
                implementation(project(":aws-runtime:aws-core"))
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.coroutines.core)

                requiredServices.forEach { implementation(project(":services:$it")) }
            }
        }
    }

    jvm {
        mainRun {
            mainClass.set("aws.sdk.kotlin.benchmarks.service.BenchmarkHarnessKt")
        }
    }
}
