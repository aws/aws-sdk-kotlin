/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections

plugins {
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

val libraries = libs

subprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            showStackTraces = true
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    apply(plugin = libraries.plugins.aws.kotlin.repo.tools.smithybuild.get().pluginId)
    apply(plugin = libraries.plugins.kotlin.jvm.get().pluginId)

    val optinAnnotations = listOf(
        "aws.smithy.kotlin.runtime.InternalApi",
        "aws.sdk.kotlin.runtime.InternalSdkApi",
    )
    kotlin.sourceSets.all {
        optinAnnotations.forEach { languageSettings.optIn(it) }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(tasks.generateSmithyProjections)
        compilerOptions.allWarningsAsErrors = false
    }

    tasks.generateSmithyProjections {
        doFirst {
            // Ensure the generated tests use the same version of the runtime as the aws aws-runtime
            val smithyKotlinRuntimeVersion = libraries.versions.smithy.kotlin.runtime.version.get()
            System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinRuntimeVersion)
        }
    }

    val implementation by configurations
    val api by configurations
    val testImplementation by configurations
    dependencies {
        codegen(project(":codegen:aws-sdk-codegen"))
        codegen(libraries.smithy.cli)
        codegen(libraries.smithy.model)

        implementation(project(":codegen:aws-sdk-codegen"))
        implementation(libraries.smithy.kotlin.codegen)

        /* We have to manually add all the dependencies of the generated client(s).
        Doing it this way (as opposed to doing what we do for protocol-tests) allows the tests to work without a
        publish to maven-local step at the cost of maintaining this set of dependencies manually. */
        implementation(libraries.kotlinx.coroutines.core)
        implementation(libraries.bundles.smithy.kotlin.service.client)
        implementation(libraries.smithy.kotlin.aws.event.stream)
        implementation(project(":aws-runtime:aws-http"))
        implementation(libraries.smithy.kotlin.aws.json.protocols)
        implementation(libraries.smithy.kotlin.serde.json)
        api(project(":aws-runtime:aws-config"))
        api(project(":aws-runtime:aws-core"))
        api(project(":aws-runtime:aws-endpoint"))

        testImplementation(libraries.kotlin.test)
        testImplementation(libraries.kotlinx.coroutines.test)
        testImplementation(libraries.smithy.kotlin.smithy.test)
        testImplementation(libraries.smithy.kotlin.aws.signing.default)
        testImplementation(libraries.smithy.kotlin.telemetry.api)
        testImplementation(libraries.smithy.kotlin.http.test)
    }
}
