/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":hll:ddb-mapper:dynamodb-mapper"))
                implementation(project(":hll:ddb-mapper:dynamodb-mapper-annotations"))
            }
        }
    }
}

dependencies {
    listOf(
        "kspCommonMainMetadata",
        "kspJvm", // FIXME Generating common code is hard for KSP: https://github.com/google/ksp/issues/567
    ).forEach { configuration -> add(configuration, project(":hll:ddb-mapper:dynamodb-mapper-annotation-processor")) }
}
