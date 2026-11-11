/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    `kotlin-dsl`
}

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("""aws\.sdk\.kotlin.*""")
        }
    }
    mavenCentral()
    gradlePluginPortal()
    maven {
        name = "kotlinRepoTools"
        url = uri("https://d2gys1nrxnjnyg.cloudfront.net/releases")
        content {
            includeGroupByRegex("""aws\.sdk\.kotlin.*""")
        }
    }
}

dependencies {
    implementation(libs.aws.kotlin.repo.tools.dokka.markdown)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(platform(libs.jackson.bom))
    // https://github.com/gradle/gradle/issues/15383 — expose generated catalog accessors to precompiled plugins
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

dependencies {
    implementation(libs.jsoup)
}
