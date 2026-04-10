/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

plugins {
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
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
                implementation(libs.kotest.runner.junit5)
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
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

            transforms = listOf(
                """
            {
                "name": "awsSmithyKotlinIncludeOperations",
                "args": {
                    "operations": [
                        "com.amazonaws.signin#CreateOAuth2Token"
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
