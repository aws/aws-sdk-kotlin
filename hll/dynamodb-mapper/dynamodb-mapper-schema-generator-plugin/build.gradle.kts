/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Plugin used to generate DynamoDbMapper schemas from user classes"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Schema Generator Plugin"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = kotlin.coreLibrariesVersion))
    implementation(libs.ksp.gradle.plugin)

    implementation(project(":hll:hll-codegen")) // for RenderOptions
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-schema-codegen")) // for AnnotationsProcessorOptions
    implementation(libs.smithy.kotlin.runtime.core) // for AttributeKey

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    website = "https://github.com/aws/aws-sdk-kotlin"
    vcsUrl = "https://github.com/aws/aws-sdk-kotlin.git"
    plugins {
        create("dynamodb-mapper-schema-generator") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator"
            displayName = "DynamoDbMapper Schema Generator"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
            tags = setOf("kotlin", "dynamodb", "aws")
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("dynamodb-mapper-schema-generator-plugin") {
            from(components["java"])
        }
    }
}

/**
 * The `java-gradle-plugin` plugin creates a javadoc jar by default, conflicting with the empty javadoc jar (emptyJar)
 * created in aws-kotlin-repo-tools. Configure dependencies and disable the emptyJar task to avoid conflicts.
 */
afterEvaluate {
    tasks.withType<PublishToMavenRepository> {
        dependsOn(tasks.named("javadocJar"))
    }

    tasks.named("publishDynamodb-mapper-schema-generatorPluginMarkerMavenPublicationToMavenLocal") {
        dependsOn(tasks.named("javadocJar"))
    }

    tasks.findByName("signDynamodb-mapper-schema-generatorPluginMarkerMavenPublication")
        ?.dependsOn(tasks.named("javadocJar"))

    tasks.named("emptyJar") {
        enabled = false
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// FIXME Commonize the following functions into the aws-kotlin-repo-tools build-support
val sdkVersion: String by project

private fun String.ensureSuffix(suffix: String): String = if (endsWith(suffix)) this else plus(suffix)

val hllPreviewVersion = if (sdkVersion.contains("-SNAPSHOT")) { // e.g. 1.3.29-beta-SNAPSHOT
    sdkVersion
        .removeSuffix("-SNAPSHOT")
        .ensureSuffix("-beta-SNAPSHOT")
} else {
    sdkVersion.ensureSuffix("-beta") // e.g. 1.3.29-beta
}

/**
 * Create a file containing the sdkVersion to use as a resource
 * This saves us from having to manually change version numbers in multiple places
 */
val generateSdkVersionFile by tasks.registering {
    val resourcesDir = layout.buildDirectory.dir("resources/main/aws/sdk/kotlin/hll/dynamodbmapper/plugins").get()
    val versionFile = file("$resourcesDir/sdk-version.txt")
    val gradlePropertiesFile = rootProject.file("gradle.properties")
    inputs.file(gradlePropertiesFile)
    outputs.file(versionFile)
    sourceSets.main.get().output.dir(resourcesDir)
    doLast {
        versionFile.writeText(hllPreviewVersion)
    }
}

/**
 * Create a file containing the Kotlin version to use as a resource
 * This saves us from having to manually change version numbers in multiple places
 */
val generateKotlinVersionFile by tasks.registering {
    val resourcesDir = layout.buildDirectory.dir("resources/main/aws/sdk/kotlin/hll/dynamodbmapper/plugins").get()
    val versionFile = file("$resourcesDir/kotlin-version.txt")
    val versionCatalogFile = rootProject.file("gradle/libs.versions.toml")
    inputs.file(versionCatalogFile)
    outputs.file(versionFile)
    sourceSets.main.get().output.dir(resourcesDir)
    doLast {
        versionFile.writeText(kotlin.coreLibrariesVersion)
    }
}

/**
 * Create a file containing the smithy-kotlin version to use as a resource
 * This saves us from having to manually change version numbers in multiple places
 */
val generateSmithyKotlinVersionFile by tasks.registering {
    val resourcesDir = layout.buildDirectory.dir("resources/main/aws/sdk/kotlin/hll/dynamodbmapper/plugins").get()
    val versionFile = file("$resourcesDir/smithy-kotlin-version.txt")
    val versionCatalogFile = rootProject.file("gradle/libs.versions.toml")
    inputs.file(versionCatalogFile)
    outputs.file(versionFile)
    sourceSets.main.get().output.dir(resourcesDir)
    doLast {
        versionFile.writeText(libs.smithy.kotlin.runtime.core.get().version.toString())
    }
}

tasks.withType<KotlinCompile> {
    dependsOn(generateSdkVersionFile)
    dependsOn(generateKotlinVersionFile)
    dependsOn(generateSmithyKotlinVersionFile)
}

tasks.withType<Test> {
    dependsOn(generateSdkVersionFile)
    dependsOn(generateKotlinVersionFile)
    dependsOn(generateSmithyKotlinVersionFile)
}

/**
 * Set up Maven Local dependencies to be used in the Gradle TestKit
 */
tasks.register("publishSmithyKotlinToMavenLocal") {
    if (gradle.includedBuilds.none { it.name == "smithy-kotlin" }) {
        return@register
    }

    val smithyKotlin = gradle.includedBuild("smithy-kotlin")

    // FIXME Simply Depend on root project's publishToMavenLocal once https://github.com/gradle/gradle/issues/22335 is fixed
    // dependsOn(smithyKotlin.task("publishToMavenLocal"))

    dependsOn(smithyKotlin.task(":runtime:auth:aws-credentials:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:auth:aws-signing-common:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:auth:aws-signing-default:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:auth:http-auth-api:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:auth:http-auth-aws:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:auth:http-auth:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:auth:identity-api:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:observability:logging-slf4j2:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:observability:telemetry-api:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:observability:telemetry-defaults:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:aws-json-protocols:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:aws-protocol-core:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:aws-xml-protocols:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:http-client-engines:http-client-engine-default:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:http-client-engines:http-client-engine-okhttp:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:http-client:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:protocol:http:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:runtime-core:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:serde:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:serde:serde-form-url:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:serde:serde-json:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:serde:serde-xml:publishToMavenLocal"))
    dependsOn(smithyKotlin.task(":runtime:smithy-client:publishToMavenLocal"))
}

tasks.withType<Test> {
    dependsOn("publishSmithyKotlinToMavenLocal")

    dependsOn(":aws-runtime:aws-config:publishToMavenLocal")
    dependsOn(":aws-runtime:aws-core:publishToMavenLocal")
    dependsOn(":aws-runtime:aws-endpoint:publishToMavenLocal")
    dependsOn(":aws-runtime:aws-http:publishToMavenLocal")
    dependsOn(":hll:dynamodb-mapper:dynamodb-mapper-annotations:publishToMavenLocal")
    dependsOn(":hll:dynamodb-mapper:dynamodb-mapper-codegen:publishToMavenLocal")
    dependsOn(":hll:dynamodb-mapper:dynamodb-mapper-schema-codegen:publishToMavenLocal")
    dependsOn(":hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:publishToMavenLocal")
    dependsOn(":hll:dynamodb-mapper:dynamodb-mapper:publishToMavenLocal")
    dependsOn(":hll:hll-codegen:publishToMavenLocal")
    dependsOn(":hll:hll-mapping-core:publishToMavenLocal")
    dependsOn(":services:dynamodb:publishToMavenLocal")
}
