/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/

description = "DynamoDbMapper schema code generation"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Codegen :: Schema"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.codegen.schema"

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `maven-publish`
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":hll:hll-codegen"))
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

val optinAnnotations = listOf(
    "aws.smithy.kotlin.runtime.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
    "kotlin.RequiresOptIn",
)

kotlin {
    explicitApi()

    sourceSets.all {
        optinAnnotations.forEach(languageSettings::optIn)
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

val sourcesJar by tasks.creating(Jar::class) {
    group = "publishing"
    description = "Assembles Kotlin sources jar"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("dynamodb-mapper-schema-codegen") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}
