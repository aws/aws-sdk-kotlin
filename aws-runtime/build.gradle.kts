/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.kotlin
import aws.sdk.kotlin.gradle.kmp.needsKmpConfigured
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

description = "AWS client runtime support for generated service clients"

plugins {
    `dokka-convention`
    alias(libs.plugins.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp) apply false
    jacoco
}

val sdkVersion: String by project

// capture locally - scope issue with custom KMP plugin
val libraries = libs

subprojects {
    if (!needsKmpConfigured) return@subprojects

    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin(libraries.plugins.aws.kotlin.repo.tools.kmp.get().pluginId)
    }

    // TODO Use configurePublishing when migrating to Sonatype Publisher API / JReleaser
    configurePublishing("aws-sdk-kotlin")

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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-Xjdk-release=1.8")
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

// Configure Dokka for subprojects
dependencies {
    subprojects.forEach {
        it.plugins.apply("dokka-convention") // Apply the Dokka conventions plugin to the subproject
        dokka(project(it.path)) // Aggregate the subprojects' generated documentation
    }

    // Preserve Dokka v1 module paths
    // https://kotlinlang.org/docs/dokka-migration.html#revert-to-the-dgp-v1-directory-behavior
    subprojects {
        dokka {
            modulePath = this@subprojects.name
        }
    }
}
val packagesToIgnore = listOf(
    "aws.sdk.kotlin.runtime.auth.credentials.internal.sts",
    "aws.sdk.kotlin.runtime.auth.credentials.internal.sso",
    "aws.sdk.kotlin.runtime.auth.credentials.internal.ssooidc",
)
apiValidation {
    ignoredPackages += packagesToIgnore
}
