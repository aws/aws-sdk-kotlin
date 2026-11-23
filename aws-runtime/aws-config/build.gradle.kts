/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

plugins {
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
    alias(libs.plugins.kotlinx.serialization)
}

description = "Support for AWS configuration"
extra["moduleName"] = "aws.sdk.kotlin.runtime.config"

apply(plugin = "org.jetbrains.kotlinx.atomicfu")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api(libs.smithy.kotlin.aws.credentials)
                api(libs.smithy.kotlin.http.client)
                implementation(libs.smithy.kotlin.http)
                implementation(libs.smithy.kotlin.http.auth)
                implementation(libs.smithy.kotlin.telemetry.api)
                implementation(libs.smithy.kotlin.http.client.engine.default)
                implementation(project(":aws-runtime:aws-http"))

                // parsing common JSON credentials responses
                implementation(libs.smithy.kotlin.serde.json)

                // additional dependencies required by generated clients
                implementation(libs.bundles.smithy.kotlin.service.client)
                implementation(project(":aws-runtime:aws-endpoint"))

                // additional dependencies required by generated sts provider
                implementation(libs.smithy.kotlin.serde.xml)
                implementation(libs.smithy.kotlin.serde.form.url)
                implementation(libs.smithy.kotlin.aws.xml.protocols)

                // additional dependencies required by generated sso provider(s)
                implementation(libs.smithy.kotlin.aws.json.protocols)

                // coroutines
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.smithy.kotlin.http.test)
                implementation(libs.smithy.kotlin.testing)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.kaml)
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}

/*
 * Unix domain sockets, used to reach the credential daemon, need the Java 16 API. The rest of this module compiles
 * against the Java 8 API (`-Xjdk-release=1.8`, see aws-runtime/build.gradle.kts), where those classes don't exist.
 *
 * The `jdk16` compilation holds only the code that calls the Java 16 API directly. It is compiled against a JDK 17
 * class library but still emits Java 8 bytecode, and its classes are packaged into the JVM jar. JVM code checks that
 * the running JVM has the API before it touches those classes (see CredentialDaemonTransportJVM.kt).
 */
val jdk17Compiler = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }

fun org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile.compileAgainstJdk17Api() {
    kotlinJavaToolchain.toolchain.use(jdk17Compiler)
    compilerOptions {
        freeCompilerArgs.set(freeCompilerArgs.get().filterNot { it.startsWith("-Xjdk-release") })
    }
}

kotlin {
    jvm {
        val main by compilations.getting
        val test by compilations.getting
        val jdk16 by compilations.creating {
            defaultSourceSet.kotlin.srcDir("jvmJdk16/src")
        }
        main.associateWith(jdk16)
        tasks.named<Jar>(artifactsTaskName) { from(jdk16.output.allOutputs) }

        // tests use the Java 16 API directly for the fake daemon's server socket; test code isn't published
        listOf(jdk16, test).forEach { compilation ->
            compilation.compileTaskProvider.configure {
                (this as org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile).compileAgainstJdk17Api()
            }
        }
    }
}

fun awsModelFile(name: String): String = rootProject.file("codegen/sdk/aws-models/$name").relativeTo(project.layout.buildDirectory.get().asFile).toString()

val codegen by configurations.getting
dependencies {
    codegen(project(":codegen:aws-sdk-codegen"))
    codegen(libs.smithy.cli)
    codegen(libs.smithy.model)
    codegen(libs.smithy.aws.smoke.test.model)
}

smithyBuild {
    val basePackage = "aws.sdk.kotlin.runtime.auth.credentials.internal"

    projections {
        // generate an sts client
        create("sts-credentials-provider") {
            imports = listOf(
                awsModelFile("sts.json"),
            )

            smithyKotlinPlugin {
                serviceShapeId = "com.amazonaws.sts#AWSSecurityTokenServiceV20110615"
                packageName = "$basePackage.sts"
                packageVersion = project.version.toString()
                packageDescription = "Internal STS credentials provider"
                sdkId = "STS"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
                }
            }

            transforms = listOf(
                """
                {
                    "name": "awsSmithyKotlinIncludeOperations",
                    "args": {
                        "operations": [
                            "com.amazonaws.sts#AssumeRole",
                            "com.amazonaws.sts#AssumeRoleWithWebIdentity"
                        ]
                    }
                }
                """,
            )
        }

        // generate an sso client
        create("sso-credentials-provider") {
            imports = listOf(
                awsModelFile("sso.json"),
            )

            val serviceShape = "com.amazonaws.sso#SWBPortalService"
            smithyKotlinPlugin {
                serviceShapeId = serviceShape
                packageName = "$basePackage.sso"
                packageVersion = project.version.toString()
                packageDescription = "Internal SSO credentials provider"
                sdkId = "SSO"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
                }
            }

            transforms = listOf(
                """
                {
                    "name": "awsSmithyKotlinIncludeOperations",
                    "args": {
                        "operations": [
                            "com.amazonaws.sso#GetRoleCredentials"
                        ]
                    }
                }
                """,
            )
        }

        create("sso-oidc-provider") {
            imports = listOf(
                awsModelFile("sso-oidc.json"),
            )

            val serviceShape = "com.amazonaws.ssooidc#AWSSSOOIDCService"
            smithyKotlinPlugin {
                serviceShapeId = serviceShape
                packageName = "$basePackage.ssooidc"
                packageVersion = project.version.toString()
                packageDescription = "Internal SSO OIDC credentials provider"
                sdkId = "SSO OIDC"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
                }
            }

            transforms = listOf(
                """
            {
                "name": "awsSmithyKotlinIncludeOperations",
                "args": {
                    "operations": [
                        "com.amazonaws.ssooidc#CreateToken"
                    ]
                }
            }
            """,
            )
        }

        create("signin-credentials-provider") {
            imports = listOf(
                awsModelFile("signin.json"),
            )

            val serviceShape = "com.amazonaws.signin#Signin"
            smithyKotlinPlugin {
                serviceShapeId = serviceShape
                packageName = "$basePackage.signin"
                packageVersion = project.version.toString()
                packageDescription = "Internal Signin credentials provider"
                sdkId = "Signin"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
                }
            }

            // FIXME `CreateOauth2TokenWithIAM` is not needed for any credentials provider and is only added because
            //  of https://github.com/smithy-lang/smithy/issues/3190
            transforms = listOf(
                """
            {
                "name": "awsSmithyKotlinIncludeOperations",
                "args": {
                    "operations": [
                        "com.amazonaws.signin#CreateOAuth2Token",
                        "com.amazonaws.signin#CreateOAuth2TokenWithIAM"
                    ]
                }
            }
            """,
            )
        }
    }
}

/*
NOTE: We need the following tasks to depend on codegen for gradle caching/up-to-date checks to work correctly:

* `compileKotlinJvm` (Type=KotlinCompile)
* `compileKotlinMetadata` (Type=KotlinCompileCommon)
* `sourcesJar` and `jvmSourcesJar` (Type=org.gradle.jvm.tasks.Jar)
*
* For Kotlin/Native, an additional dependency is introduced:
* `compileKotlin<Platform>` (Type=KotlinNativeCompile) (e.g. compileKotlinLinuxX64)
*/
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(tasks.generateSmithyProjections)

    compilerOptions {
        // generated sts/sso credential providers have quite a few warnings
        allWarningsAsErrors.set(false)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    dependsOn(tasks.generateSmithyProjections)
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon> {
    dependsOn(tasks.generateSmithyProjections)
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    if (name == "jar") {
        println("Disabling $project task '$name' because it conflicts with Kotlin JAR tasks")
        enabled = false
    } else {
        dependsOn(tasks.generateSmithyProjections)
    }
}

smithyBuild.projections.all {
    // add this projected source dir to the common sourceSet
    val projectionSrcDir = smithyBuild.smithyKotlinProjectionSrcDir(name)
    kotlin.sourceSets.commonMain {
        logger.info("added $projectionSrcDir to common sourceSet")
        kotlin.srcDir(projectionSrcDir)
    }
}

// Suppress internally-generated clients
dokka {
    tasks.dokkaGenerateModuleHtml {
        dependsOn(tasks.generateSmithyProjections)
    }

    dokkaSourceSets.configureEach {
        perPackageOption {
            matchingRegex.set(""".*\.internal.*""")
            suppress.set(true)
        }
    }
}
