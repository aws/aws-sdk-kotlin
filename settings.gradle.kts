/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "kotlinRepoTools"
            url = java.net.URI("https://d2gys1nrxnjnyg.cloudfront.net/releases")
            content {
                includeGroupByRegex("""aws\.sdk\.kotlin.*""")
            }
        }
    }
    resolutionStrategy {
        val sdkVersion: String by settings
        eachPlugin {
            if (requested.id.id == "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator") {
                useModule("aws.sdk.kotlin:dynamodb-mapper-schema-generator-plugin:$sdkVersion")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "kotlinRepoTools"
            url = java.net.URI("https://d2gys1nrxnjnyg.cloudfront.net/releases")
            content {
                includeGroupByRegex("""aws\.sdk\.kotlin.*""")
            }
        }
    }
}

rootProject.name = "aws-sdk-kotlin"

includeBuild("build-support")

include(":dokka-aws")
include(":bom")
include(":codegen:sdk")
include(":codegen:aws-sdk-codegen")
include(":codegen:protocol-tests")
include(":aws-runtime")
include(":aws-runtime:aws-core")
include(":aws-runtime:aws-config")
include(":aws-runtime:aws-endpoint")
include(":aws-runtime:aws-http")
include(":hll")
include(":hll:hll-codegen")
include(":hll:hll-mapping-core")
include(":services")
include(":tests")
include(":tests:codegen")
include(":tests:codegen:event-stream")
include(":tests:codegen:rules-engine")
include(":tests:e2e-test-util")
include(":tests:codegen:smoke-tests")
include(":tests:codegen:checksums")

// generated services
val File.isServiceDir: Boolean
    get() = isDirectory && toPath().resolve("build.gradle.kts").toFile().exists()

val String.isBootstrappedService: Boolean
    get() = file("services/$this").isServiceDir

file("services").listFiles().forEach {
    if (it.isServiceDir) {
        include(":services:${it.name}")
    }
}

if ("dynamodb".isBootstrappedService) {
    include(":hll:dynamodb-mapper")
    include(":hll:dynamodb-mapper:dynamodb-mapper")
    include(":hll:dynamodb-mapper:dynamodb-mapper-codegen")
    include(":hll:dynamodb-mapper:dynamodb-mapper-ops-codegen")
    include(":hll:dynamodb-mapper:dynamodb-mapper-schema-codegen")
    include(":hll:dynamodb-mapper:dynamodb-mapper-annotations")
    include(":hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin")
} else {
    logger.warn(":services:dynamodb is not bootstrapped, skipping :hll:dynamodb-mapper and subprojects")
}

// Service benchmarks project
val benchmarkServices = listOf(
    // keep this list in sync with tests/benchmarks/service-benchmarks/build.gradle.kts

    "s3",
    "sns",
    "sts",
    "cloudwatch",
    "cloudwatchevents",
    "dynamodb",
    "secretsmanager",
    "iam",
)
if (benchmarkServices.all { it.isBootstrappedService }) {
    include(":tests:benchmarks:service-benchmarks")
} else {
    val missing = benchmarkServices.filterNot { it.isBootstrappedService }
    logger.warn("Skipping :tests:benchmarks:service-benchmarks because these service(s) are not bootstrapped: $missing")
}

/**
 * The following code enables to optionally include aws-sdk-kotlin dependencies in source form for easier
 * development.  By default, if `smithy-kotlin` exists as a directory at the same level as `aws-sdk-kotlin`
 * then `smithy-kotlin` will be added as a composite build.  To override this behavior, for example to add
 * more composite builds, specify a different directory for `smithy-kotlin`, or to disable the feature entirely,
 * a local.properties file can be added or amended such that the property `compositeProjects` specifies
 * a comma delimited list of paths to project roots that shall be added as composite builds.  If the list is
 * empty to builds will be added.  Invalid directories are ignored.  Example local.properties:
 *
 * compositeProjects=~/repos/smithy-kotlin,/tmp/some/other/thing,../../another/project
 *
 */
val compositeProjectList = try {
    val localProperties = java.util.Properties()
    localProperties.load(File(rootProject.projectDir, "local.properties").inputStream())
    val propertyVal = localProperties.getProperty("compositeProjects") ?: "../smithy-kotlin"
    val filePaths = propertyVal
        .splitToSequence(",") // Split comma delimited string into sequence
        .map { it.replaceFirst("^~".toRegex(), System.getProperty("user.home")) } // expand user dir
        .filter { it.isNotBlank() }
        .map { file(it) } // Create file from path
        .toList()

    if (filePaths.isNotEmpty()) println("Adding ${filePaths.size} composite build directories from local.properties.")
    filePaths
} catch (e: java.io.FileNotFoundException) {
    listOf(file("../smithy-kotlin")) // Default path, not an error.
} catch (e: Throwable) {
    logger.error("Failed to load project paths from local.properties. Assuming defaults.", e)
    listOf(file("../smithy-kotlin"))
}

compositeProjectList.forEach { projectRoot ->
    when (projectRoot.exists()) {
        true -> {
            println("Including build '$projectRoot'")
            includeBuild(projectRoot)
        }
        false -> println("Ignoring invalid build directory '$projectRoot'.")
    }
}
