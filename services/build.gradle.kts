/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import aws.sdk.kotlin.gradle.kmp.*
import aws.sdk.kotlin.gradle.util.typedProp
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime

plugins {
    `maven-publish`
    `dokka-convention`
    alias(libs.plugins.aws.kotlin.repo.tools.kmp) apply false
}

val sdkVersion: String by project

val optinAnnotations = listOf(
    "aws.smithy.kotlin.runtime.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
    "kotlin.RequiresOptIn",
)

// capture locally - scope issue with custom KMP plugin
val libraries = libs

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin(libraries.plugins.aws.kotlin.repo.tools.kmp.get().pluginId)
    }

    logger.info("configuring: $project")

    kotlin {
        explicitApi()

        sourceSets {
            all {
                // have generated sdk's opt-in to internal runtime features
                optinAnnotations.forEach { languageSettings.optIn(it) }
            }

            getByName("commonMain") {
                kotlin.srcDir("generated-src/main/kotlin")
            }

            getByName("commonTest") {
                kotlin.srcDir("generated-src/test")

                dependencies {
                    implementation(libraries.kotlinx.coroutines.test)
                    implementation(libraries.smithy.kotlin.http.test)
                }
            }
        }

        if (project.file("e2eTest").exists()) {
            jvm().compilations {
                val e2eTest by creating {
                    defaultSourceSet {
                        kotlin.srcDir("e2eTest/src")
                        resources.srcDir("e2eTest/test-resources")
                        dependsOn(this@kotlin.sourceSets.getByName("commonMain"))
                        dependsOn(this@kotlin.sourceSets.getByName("jvmMain"))

                        dependencies {
                            api(libraries.smithy.kotlin.testing)
                            implementation(libraries.kotlin.test)
                            implementation(libraries.kotlin.test.junit5)
                            implementation(project(":tests:e2e-test-util"))
                            implementation(libraries.slf4j.simple)
                        }
                    }

                    tasks.register<Test>("e2eTest") {
                        description = "Run e2e service tests"
                        group = "verification"

                        if (project.name == "s3") {
                            dependencies {
                                implementation(project(":services:s3control"))
                                implementation(project(":services:sts"))
                                implementation(libs.smithy.kotlin.aws.signing.crt)
                            }
                        }

                        if (project.name == "sesv2") {
                            dependencies {
                                implementation(libs.smithy.kotlin.aws.signing.crt) // needed for E2E test of SigV4a
                            }
                        }

                        if (project.name == "route53") {
                            dependencies {
                                implementation(libraries.smithy.kotlin.http.test) // needed for URI E2E tests
                            }
                        }

                        // Run the tests with the classpath containing the compile dependencies (including 'main'),
                        // runtime dependencies, and the outputs of this compilation:
                        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs

                        // Run only the tests from this compilation's outputs:
                        testClassesDirs = output.classesDirs

                        useJUnitPlatform()
                        testLogging {
                            events("passed", "skipped", "failed")
                            showStandardStreams = true
                            showStackTraces = true
                            showExceptions = true
                            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                        }

                        // model a random input to enable re-running e2e tests back to back without
                        // up-to-date checks or cache getting in the way
                        inputs.property("integration.datetime", LocalDateTime.now())
                        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN"))
                    }
                }
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            allWarningsAsErrors.set(false) // FIXME Tons of errors occur in generated code
            jvmTarget.set(JvmTarget.JVM_1_8) // fixes outgoing variant metadata: https://github.com/smithy-lang/smithy-kotlin/issues/258
            freeCompilerArgs.add("-Xjdk-release=1.8")
        }
    }

    // TODO Use configurePublishing when migrating to Sonatype Publisher API / JReleaser
    configurePublishing("aws-sdk-kotlin")

    publishing {
        publications.all {
            if (this !is MavenPublication) return@all
            project.afterEvaluate {
                val sdkId = project.typedProp<String>("aws.sdk.id") ?: error("service build `${project.name}` is missing `aws.sdk.id` property required for publishing")
                pom.properties.put("aws.sdk.id", sdkId)
            }
        }
    }
}

// Configure Dokka for subprojects
dependencies {
    subprojects.forEach {
        it.plugins.apply("dokka-convention") // Apply the Dokka conventions plugin to the subproject
        dokka(project(it.path)) // Aggregate the subproject's generated documentation
    }

    // Preserve Dokka v1 module paths
    // https://kotlinlang.org/docs/dokka-migration.html#revert-to-the-dgp-v1-directory-behavior
    subprojects {
        dokka {
            modulePath = this@subprojects.name
        }
    }
}
