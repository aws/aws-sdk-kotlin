/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "aws-sdk-kotlin internal build scripts"
extra["moduleName"] = "aws.sdk.kotlin.build"

plugins {
    kotlin("jvm")
    application
}

repositories {
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3")

    val smithyVersion: String by project
    implementation("software.amazon.smithy:smithy-codegen-core:$smithyVersion")
    implementation("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
}

val cliMainClass:String = "aws.sdk.kotlin.build.MainKt"

application {
    mainClassName = cliMainClass
}

tasks.run.configure {
    val rootProjectDir = rootProject.projectDir.absolutePath
    workingDir(rootProjectDir)
}


