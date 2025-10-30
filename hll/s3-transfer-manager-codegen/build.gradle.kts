/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val sdkVersion: String by project
version = sdkVersion

description = "S3 Transfer Manager Code Generation"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: S3 Transfer Manager Codegen"
extra["moduleName"] = "aws.sdk.kotlin.hll.s3transfermanager.codegen"

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":hll:hll-codegen"))
    implementation(project(":services:s3"))
}

kotlin {
    explicitApi()
    sourceSets.all {
        listOf(
            "aws.smithy.kotlin.runtime.InternalApi",
            "aws.sdk.kotlin.runtime.InternalSdkApi",
            "kotlin.RequiresOptIn",
        ).forEach(languageSettings::optIn)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=1.8")
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
