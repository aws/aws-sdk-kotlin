/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

description = "High-level libraries for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL"
extra["moduleName"] = "aws.sdk.kotlin.hll"

// FIXME 🔽🔽🔽 This is all copied from :aws-runtime and should be commonized 🔽🔽🔽

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp) apply false
    jacoco
}

val sdkVersion: String by project

// capture locally - scope issue with custom KMP plugin
val libraries = libs

// General subproject configuration
subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    configurePublishing("aws-sdk-kotlin")
}

// KMP-specific configuration
subprojects {
    if (!needsKmpConfigured) {
        return@subprojects
    }

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin("org.jetbrains.dokka")
        plugin(libraries.plugins.aws.kotlin.repo.tools.kmp.get().pluginId)
    }

    kotlin {
        explicitApi()

        sourceSets {
            // dependencies available for all subprojects

            named("commonTest") {
                dependencies {
                    implementation(libraries.kotest.assertions.core)
                }
            }

            named("jvmTest") {
                dependencies {
                    implementation(libraries.kotest.assertions.core.jvm)
                    implementation(libraries.slf4j.simple)
                }
            }
        }
    }

    kotlin.sourceSets.all {
        // Allow subprojects to use internal APIs
        // See https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
        listOf("kotlin.RequiresOptIn").forEach { languageSettings.optIn(it) }
    }

    dependencies {
        dokkaPlugin(project(":dokka-aws"))
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

apiValidation {
    val availableSubprojects = subprojects.map { it.name }.toSet()

    ignoredProjects += listOf(
        "hll-codegen",
        "dynamodb-mapper-annotation-processor-test",
        "dynamodb-mapper-codegen",
    ).filter { it in availableSubprojects } // Some projects may not be in the build depending on bootstrapping
}
