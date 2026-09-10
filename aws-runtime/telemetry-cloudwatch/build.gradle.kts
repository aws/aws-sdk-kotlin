/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
description = "CloudWatch metrics exporter for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: Telemetry :: CloudWatch"
extra["moduleName"] = "aws.sdk.kotlin.runtime.telemetry.cloudwatch"

// Multiplatform atomics for the attach and shutdown guards, matching how aws-config gets them.
apply(plugin = "org.jetbrains.kotlinx.atomicfu")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // `api` because the exporter's builder exposes `client: CloudWatchClient?`, so callers
                // who supply their own client must be able to name the type.
                //
                // This dependency on a generated service client is also why this module is included
                // conditionally in settings.gradle.kts: it can only be built when `:services:cloudwatch`
                // has been bootstrapped, the same way `:hll:ddb-mapper` depends on `:services:dynamodb`.
                api(project(":services:cloudwatch"))

                // `api` because CloudWatchMetricExporter IS-A MetricExporter and is handed to
                // SdkTelemetryProvider's builder; both types have to be visible to callers.
                api(libs.smithy.kotlin.telemetry.metrics.aggregation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
