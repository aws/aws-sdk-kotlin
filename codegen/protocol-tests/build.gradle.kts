/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

plugins {
    kotlin("jvm")
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
}

description = "Smithy protocol test suite"

data class ProtocolTest(val projectionName: String, val serviceShapeId: String, val sdkId: String? = null) {
    val packageName: String = projectionName.lowercase().filter { it.isLetterOrDigit() }
}

// The following section exposes Smithy protocol test suites as gradle test targets
// for the configured protocols in [enabledProtocols].
val enabledProtocols = listOf(
    // service specific tests
    ProtocolTest("apigateway", "com.amazonaws.apigateway#BackplaneControlService"),
    ProtocolTest("glacier", "com.amazonaws.glacier#Glacier"),
    ProtocolTest("machinelearning", "com.amazonaws.machinelearning#AmazonML_20141212", sdkId = "Machine Learning"),
)

smithyBuild {
    enabledProtocols.forEach { test ->
        projections.register(test.projectionName) {
            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["${test.serviceShapeId}"]
                  }
                }
                """,
            )

            smithyKotlinPlugin {
                serviceShapeId = test.serviceShapeId
                packageName = "aws.sdk.kotlin.services.${test.packageName}"
                packageVersion = "1.0"
                sdkId = test.sdkId
                buildSettings {
                    generateFullProject = true
                    optInAnnotations = listOf(
                        "aws.smithy.kotlin.runtime.InternalApi",
                        "aws.sdk.kotlin.runtime.InternalSdkApi",
                    )
                }
                apiSettings {
                    defaultValueSerializationMode = "always"
                }
            }
        }
    }
}

val codegen by configurations.getting
dependencies {
    codegen(project(":codegen:aws-sdk-codegen"))
    codegen(libs.smithy.cli)
    codegen(libs.smithy.model)

    // NOTE: The protocol tests are published to maven as a jar, this ensures that
    // the aws-protocol-tests dependency is found when generating code such that the `includeServices` transform
    // actually works
    codegen(libs.smithy.aws.protocol.tests)
}

smithyBuild.projections.forEach {
    val protocolName = it.name

    val dirProvider = smithyBuild
        .smithyKotlinProjectionPath(protocolName)
        .map { file(it.toString()) }

    val copyStaticFiles = tasks.register<Copy>("copyStaticFiles-$protocolName") {
        group = "codegen"
        from(rootProject.projectDir.resolve("services/$protocolName/common/src"))
        into(smithyBuild.smithyKotlinProjectionSrcDir(protocolName))
    }

    tasks.register<Exec>("testProtocol-$protocolName") {
        group = "Verification"
        dependsOn(tasks.generateSmithyProjections, copyStaticFiles)

        doFirst {
            val dir = dirProvider.get()
            require(dir.exists()) { "$dir does not exist" }

            val wrapper = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "gradlew"
            val gradlew = rootProject.layout.projectDirectory.file(wrapper).asFile.absolutePath

            workingDir = dir
            executable = gradlew
            args = listOf("test")
        }
    }
}

tasks.register("testAllProtocols") {
    group = "Verification"
    dependsOn(tasks.matching { it.name.startsWith("testProtocol-") })
}
