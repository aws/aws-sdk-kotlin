/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.kmp.NATIVE_ENABLED
import com.google.devtools.ksp.gradle.KspTaskJvm
import com.google.devtools.ksp.gradle.KspTaskMetadata
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.sourceSets
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.nio.file.Files
import java.nio.file.StandardCopyOption

description = "S3 Transfer Manager for the AWS SDK for Kotlin"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: S3 Transfer Manager"
extra["moduleName"] = "aws.sdk.kotlin.hll.s3transfermanager"

plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-http"))
                implementation(project(":services:s3"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.smithy.kotlin.test.jvm)
                implementation(libs.smithy.kotlin.testing.jvm)
            }
        }
    }
}

ksp {
    dependencies {
        ksp(project(":hll:s3-transfer-manager-codegen"))
    }
}

// This is copied from :hll:dynamodb-mapper:dynamodb-mapper. TODO: Commonize
if (project.NATIVE_ENABLED) {
    // Configure KSP for multiplatform: https://kotlinlang.org/docs/ksp-multiplatform.html
    // https://github.com/google/ksp/issues/963#issuecomment-1894144639
    // https://github.com/google/ksp/issues/965
    kotlin.sourceSets.commonMain {
        tasks.withType<KspTaskMetadata> {
            // Wire up the generated source to the commonMain source set
            kotlin.srcDir(destinationDirectory)
        }
    }
} else {
    // FIXME This is a dirty hack for JVM-only builds which KSP doesn't consider to be "multiplatform". Explanation of
    //  hack follows in narrative, minimally-opinionated comments.

    // Then we need to move the generated source from jvm to common
    val moveGenSrc by tasks.registering {
        // Can't move src until the src is generated
        dependsOn(tasks.named("kspKotlinJvm"))

        // Detecting these paths programmatically is complex; just hardcode them
        val srcDir = file("build/generated/ksp/jvm/jvmMain")
        val destDir = file("build/generated/ksp/common/commonMain")

        inputs.dir(srcDir)
        outputs.dirs(srcDir, destDir)

        doLast {
            if (destDir.exists()) {
                // Clean out the existing destination, otherwise move fails
                require(destDir.deleteRecursively()) { "Failed to delete $destDir before moving from $srcDir" }
            } else {
                // Create the destination directories, otherwise move fails
                require(destDir.mkdirs()) { "Failed to create path $destDir" }
            }

            Files.move(srcDir.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    listOf("jvmSourcesJar", "metadataSourcesJar", "jvmProcessResources").forEach {
        tasks.named(it) {
            dependsOn(moveGenSrc)
        }
    }

    tasks.withType<KotlinCompilationTask<*>> {
        if (this !is KspTaskJvm) {
            // Ensure that any **non-KSP** compile tasks depend on the generated src move
            dependsOn(moveGenSrc)
        }
    }

    // Finally, wire up the generated source to the commonMain source set
    kotlin.sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/common/commonMain/kotlin")
    }
}
