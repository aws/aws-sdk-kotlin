/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(platform(libs.jackson.bom))
    // https://github.com/gradle/gradle/issues/15383 — expose generated catalog accessors to precompiled plugins
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

dependencies {
    implementation(libs.jsoup)
}
