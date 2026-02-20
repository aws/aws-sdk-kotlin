/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.kmp.*
import aws.sdk.kotlin.gradle.publishing.configurePublishing
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
            sourceSets {
                val e2eTest by creating {
                    kotlin.srcDir("e2eTest/src/commonMain")
                    resources.srcDir("e2eTest/test-resources")
                    dependsOn(this@kotlin.sourceSets.getByName("commonMain"))

                    dependencies {
                        api(libraries.smithy.kotlin.testing)
                        implementation(libraries.kotlin.test)
                        implementation(libraries.kotlinx.coroutines.test)
                        implementation(libraries.smithy.kotlin.http.test)
                        implementation(project(":tests:e2e-test-util"))
                    }

                    if (project.name == "s3") {
                        dependencies {
                            rootProject.findProject(":services:s3control")?.let { implementation(it) }
                            rootProject.findProject(":services:sts")?.let { implementation(it) }
                        }
                    }
                }
            }

            jvm().compilations {
                val e2eTest by creating {
                    defaultSourceSet {
                        kotlin.srcDir("e2eTest/src/jvmMain")
                        dependsOn(this@kotlin.sourceSets["e2eTest"])
                        dependsOn(this@kotlin.sourceSets.getByName("jvmMain"))
                        dependencies {
                            implementation(libraries.slf4j.simple)
                            implementation(libraries.kotlin.test.junit5)
                            implementation(libraries.smithy.kotlin.aws.signing.crt)
                        }

                        if (project.name == "sesv2") {
                            dependencies {
                                implementation(libraries.smithy.kotlin.aws.signing.crt)
                            }
                        }

                        if (project.name == "route53") {
                            dependencies {
                                implementation(libraries.smithy.kotlin.http.test)
                            }
                        }
                    }

                    tasks.register<Test>("jvmE2eTest") {
                        description = "Run JVM E2E service tests"
                        group = "verification"
                        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
                        testClassesDirs = output.classesDirs
                        useJUnitPlatform()
                        testLogging {
                            events("passed", "skipped", "failed")
                            showStandardStreams = true
                            showStackTraces = true
                            showExceptions = true
                            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                        }
                        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN"))
                    }
                }
            }

            targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
                val target = this
                target.compilations {
                    val e2eTest by creating {
                        defaultSourceSet {
                            kotlin.srcDir("e2eTest/src/nativeMain")
                            dependsOn(this@kotlin.sourceSets["e2eTest"])
                            dependsOn(this@kotlin.sourceSets.getByName("nativeMain"))
                            dependencies {
                                implementation(project(":tests:e2e-test-util"))
                            }
                        }
                    }
                }

                binaries.test("e2eTest", listOf(DEBUG)) {
                    compilation = target.compilations.getByName("e2eTest")
                }

                tasks.register<Exec>("${target.targetName}E2eTest") {
                    description = "Run ${target.targetName} E2E service tests"
                    group = "verification"
                    val linkTaskName = "linkE2eTestDebugTest${target.targetName.replaceFirstChar { it.uppercase() }}"
                    dependsOn(linkTaskName)
                    executable = "build/bin/${target.targetName}/e2eTestDebugTest/e2eTest.kexe"

                    // TODO More consideration needed for running E2E tests on iOS... booting simulator, configuring credentials inside the sim, etc.
                    onlyIf { !target.targetName.startsWith("ios") }
                }
            }

            tasks.register("nativeE2eTest") {
                description = "Run Native E2E service tests"
                group = "verification"
                targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().forEach {
                    dependsOn("${it.targetName}E2eTest")
                }
            }

            tasks.register("e2eTest") {
                dependsOn("jvmE2eTest")
                // dependsOn("nativeE2eTest") // FIXME Figure out how we want to run E2E tests (same task as JVM, different tasks, matrixed by target or just one Native target, etc.)
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
