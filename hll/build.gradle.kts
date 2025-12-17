/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.kotlin
import aws.sdk.kotlin.gradle.kmp.needsKmpConfigured
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

description = "High-level libraries for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL"
extra["moduleName"] = "aws.sdk.kotlin.hll"

// FIXME 🔽🔽🔽 This is all copied from :aws-runtime and should be commonized 🔽🔽🔽

plugins {
    `dokka-convention`
    alias(libs.plugins.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp) apply false
    jacoco
}

val sdkVersion: String by project

// capture locally - scope issue with custom KMP plugin
val libraries = libs

val optinAnnotations = listOf(
    "aws.smithy.kotlin.runtime.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
    "kotlin.RequiresOptIn",
)

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion
    // TODO Use configurePublishing when migrating to Sonatype Publisher API / JReleaser
    configurePublishing("aws-sdk-kotlin")
}

subprojects {
    if (!needsKmpConfigured) {
        return@subprojects
    }

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin(libraries.plugins.aws.kotlin.repo.tools.kmp.get().pluginId)
    }

    kotlin {
        explicitApi()

        sourceSets {
            // dependencies available for all subprojects

            all {
                optinAnnotations.forEach(languageSettings::optIn)
            }

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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xjdk-release=1.8")
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

// Projects to ignore for API validation and documentation generation
val projectsToIgnore = listOf(
    "hll-codegen",
    "dynamodb-mapper-codegen",
    "dynamodb-mapper-ops-codegen",
    "dynamodb-mapper-schema-codegen",
).filter { it in subprojects.map { it.name }.toSet() } // Some projects may not be in the build depending on bootstrapping

apiValidation {
    ignoredProjects += projectsToIgnore
    nonPublicMarkers += "aws.smithy.kotlin.runtime.GeneratedApi"
}

// Configure Dokka for subprojects
dependencies {
    subprojects.forEach {
        if (it.name !in projectsToIgnore) {
            it.plugins.apply("dokka-convention") // Apply the Dokka conventions plugin to the subproject
            dokka(project(it.path)) // Aggregate the subproject's generated documentation
        }
    }

    // Preserve Dokka v1 module paths
    // https://kotlinlang.org/docs/dokka-migration.html#revert-to-the-dgp-v1-directory-behavior
    subprojects {
        val subProjectName = this@subprojects.name

        if (subProjectName in projectsToIgnore) {
            return@subprojects
        }

        dokka {
            modulePath = subProjectName
        }
    }
}
