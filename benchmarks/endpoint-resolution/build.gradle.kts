/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("org.jetbrains.kotlinx.benchmark") version libs.versions.kotlinx.benchmark.version.get()
    id("org.jetbrains.kotlin.plugin.allopen") version libs.versions.kotlin.version.get()
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

dependencies {
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":services:s3"))
    implementation(project(":services:lambda"))
}

val asyncProfilerLib: String? = providers.gradleProperty("asyncProfiler.libPath").orNull

benchmark {
    targets {
        register("main")
    }
    configurations {
        named("main") {
            reportFormat = "json"
        }
        if (asyncProfilerLib != null) {
            register("profile") {
                reportFormat = "json"
                profiler = "async:libPath=$asyncProfilerLib;output=flamegraph;dir=build/profiles"
            }
        }
    }
}
