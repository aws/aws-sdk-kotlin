/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

// Throwaway spike: measures which Gradle versions can load and run a plugin built the same way as
// dynamodb-mapper-schema-generator-plugin (kotlin-dsl + java-gradle-plugin, built by this repo's Gradle and KGP).

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "aws.sdk.kotlin"
version = "0.0.1-spike"

// Released SDK plugins are class file 61 (JDK 17) only because release CI builds on JDK 17. Pin it here so a
// local JDK 21 build matches the release.
kotlin { jvmToolchain(17) }

// Project repositories replace the settings ones, so list Maven Central again.
repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("gradle-plugin", version = kotlin.coreLibrariesVersion))
    // Oldest AGP that KGP 2.4.21 supports; newer AGPs must stay binary compatible with what we call.
    compileOnly("com.android.tools.build:gradle-api:8.5.2")
    compileOnly("com.google.code.gson:gson:2.11.0")

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
}

gradlePlugin {
    plugins {
        create("custom-client-spike") {
            id = "aws.sdk.kotlin.custom-client-spike"
            implementationClass = "aws.sdk.kotlin.spike.customclient.CustomClientSpikePlugin"
        }
    }
}

val spikeRepo = layout.buildDirectory.dir("spike-repo")

publishing {
    repositories {
        maven {
            name = "spike"
            url = uri(spikeRepo)
        }
    }
}

val gradleVersions = (findProperty("spike.gradleVersions") as String?)
    ?: "7.6.3,8.0.2,8.2.1,8.4,8.6,8.10.2,8.14.3,9.0.0,9.7.1,9.8.1"

val testJdk = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }

tasks.test {
    useJUnitPlatform()
    dependsOn("publishAllPublicationsToSpikeRepository")
    // Gradle 7.6.3 can't run on JDK 21+, and Gradle 9 needs JDK 17+, so JDK 17 covers the whole matrix.
    javaLauncher.set(testJdk)
    systemProperty("spike.repo", spikeRepo.get().asFile.absolutePath)
    systemProperty("spike.version", version.toString())
    systemProperty("spike.gradleVersions", gradleVersions)
    systemProperty("spike.kotlinVersion", kotlin.coreLibrariesVersion)
    systemProperty("spike.workDir", layout.buildDirectory.dir("matrix").get().asFile.absolutePath)
    // AGP version @ Gradle version, picked from AGP's minimum-Gradle table.
    systemProperty(
        "spike.agpMatrix",
        (findProperty("spike.agpMatrix") as String?) ?: "8.5.2@8.7,8.13.2@8.14.3,9.0.1@9.1.0,9.3.3@9.7.1,9.5.1@9.8.1",
    )
    systemProperty("spike.androidSdk", (findProperty("spike.androidSdk") as String?) ?: "${System.getProperty("user.home")}/android-sdk")
    outputs.upToDateWhen { false }
    maxHeapSize = "1g"
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}
