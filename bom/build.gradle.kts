/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

plugins {
    `maven-publish`
    `java-platform`
    `version-catalog`
}

val sdkVersion: String by project

group = "aws.sdk.kotlin"
version = sdkVersion
description = "Provides a BOM"

val evaluateAfter = listOf(":services", ":aws-runtime", ":tests", ":codegen")
evaluateAfter.forEach { evaluationDependsOn(it) }

fun createBomConstraintsAndVersionCatalog() {
    val bomConstraints: DependencyConstraintHandler = dependencies.constraints
    val catalogExt = catalog

    rootProject.subprojects {
        val subproject = this
        val hasMavenPublish = subproject.plugins.hasPlugin("maven-publish")
        if (!hasMavenPublish) {
            logger.info("skipping bom and version-catalog entry for ${subproject.name}")
            return@subprojects
        }
        subproject.plugins.withType<KotlinMultiplatformPluginWrapper> {
            subproject.extensions.getByType<KotlinMultiplatformExtension>().targets.all {
                val target = this
                val gavCoordinates = gav(target)
                bomConstraints.api(gavCoordinates)
                catalogExt.versionCatalog {
                    val prefix = when {
                        subproject.path.contains(":services") -> "services-"
                        subproject.path.contains(":aws-runtime") -> "runtime-"
                        else -> ""
                    }
                    val alias = prefix + artifactId(target)
                    library(alias, gavCoordinates)
                }
            }
        }
    }

    // Add the BOM itself to the version catalog
    catalogExt.versionCatalog {
        library("bom", "aws.sdk.kotlin:bom:$version")
    }

    val ignoredSmithyKotlin = setOf(
        "smithy.kotlin.codegen",
        "smithy.kotlin.http.test",
        "smithy.kotlin.test",
        "smithy.kotlin.smithy.test",
        "smithy.kotlin.aws.signing.test",
    )

    // add smithy-kotlin versions to our BOM and allow direct aliasing in the catalog
    catalogExt.versionCatalog {
        val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
        libsCatalog.libraryAliases
            .filter {
                it.startsWith("smithy.kotlin") && ignoredSmithyKotlin.none { prefix -> it.startsWith(prefix) }
            }.forEach { alias ->
                val coordinates = libsCatalog.findLibrary(alias).get()
                bomConstraints.api(coordinates)
                val newAlias = "runtime-${alias.replace('.', '-')}"
                library(newAlias, coordinates.get().toString())
            }
    }
}

fun Project.artifactId(target: KotlinTarget): String = when (target) {
    is KotlinMetadataTarget -> name
    else -> "$name-${target.targetName.lowercase()}"
}

/**
 * Returns a string like "aws.sdk.kotlin:s3-linuxx64:1.0.2" for this target.
 */
fun Project.gav(target: KotlinTarget): String {
    val artifactId = artifactId(target)
    return "$group:$artifactId:$version"
}

fun DependencyConstraintHandler.api(constraintNotation: Any) =
    add("api", constraintNotation)

createBomConstraintsAndVersionCatalog()

// TODO Use configurePublishing when migrating to Sonatype Publisher API / JReleaser
configurePublishing("aws-sdk-kotlin")

publishing {
    publications {
        create("bom", MavenPublication::class) {
            artifactId = "bom"
            from(project.components.getByName("javaPlatform"))
        }

        create<MavenPublication>("versionCatalog") {
            artifactId = "version-catalog"
            description = "Provides a version catalog"
            from(components["versionCatalog"])
        }
    }
}
