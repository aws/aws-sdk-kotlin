/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp)
}

description = "Test utilities for integration and e2e tests"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.smithy.kotlin.http.client.engine.crt)
            }
        }
        
        jvmMain {
            dependencies {
                api(libs.smithy.kotlin.http.client.engine.default)
                implementation(libs.smithy.kotlin.http.client.engine.okhttp)
                implementation(libs.smithy.kotlin.http.client.engine.okhttp4)
            }
        }
    }
}
