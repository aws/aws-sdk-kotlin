/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.kmp.NATIVE_ENABLED
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer
import com.google.devtools.ksp.gradle.KspTaskJvm
import com.google.devtools.ksp.gradle.KspTaskMetadata
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.properties.Delegates

description = "High level DynamoDbMapper client"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper"

buildscript {
    dependencies {
        classpath(libs.ddb.local)
    }
}

plugins {
    alias(libs.plugins.ksp)
    `dokka-convention`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":aws-runtime:aws-http"))
                api(project(":services:dynamodb"))
                api(project(":hll:hll-mapping-core"))
                api(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}

ksp {
    arg("pkg", "aws.sdk.kotlin.hll.dynamodbmapper.operations")

    val allowlist = listOf(
        "deleteItem",
        "getItem",
        "putItem",
        "query",
        "scan",
    )
    arg("op-allowlist", allowlist.joinToString(";"))
}

if (project.NATIVE_ENABLED) {
    // Configure KSP for multiplatform: https://kotlinlang.org/docs/ksp-multiplatform.html
    // https://github.com/google/ksp/issues/963#issuecomment-1894144639
    // https://github.com/google/ksp/issues/965
    dependencies.kspCommonMainMetadata(project(":hll:dynamodb-mapper:dynamodb-mapper-ops-codegen"))

    kotlin.sourceSets.commonMain {
        tasks.withType<KspTaskMetadata> {
            // Wire up the generated source to the commonMain source set
            kotlin.srcDir(destinationDirectory)
        }
    }
} else {
    // FIXME This is a dirty hack for JVM-only builds which KSP doesn't consider to be "multiplatform". Explanation of
    //  hack follows in narrative, minimally-opinionated comments.

    // Start by invoking the JVM-only KSP configuration
    dependencies.kspJvm(project(":hll:dynamodb-mapper:dynamodb-mapper-ops-codegen"))

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

open class DynamoDbLocalInstance : DefaultTask() {
    private var port: Int by Delegates.notNull()

    @OutputFile
    val portFile = project.objects.fileProperty()

    @Internal
    var runner: DynamoDBProxyServer? = null
        private set

    @TaskAction
    fun exec() {
        port = ServerSocket(0).use { it.localPort }

        println("Starting DynamoDB local instance on port $port")
        runner = ServerRunner
            .createServerFromCommandLineArgs(arrayOf("-inMemory", "-port", port.toString(), "-disableTelemetry"))
            .also { it.start() }

        portFile
            .asFile
            .get()
            .also { println("Writing port info file to ${it.absolutePath}") }
            .writeText(port.toString())
    }

    fun stop() {
        runCatching {
            portFile
                .asFile
                .get()
                .also { println("Deleting port info file at ${it.absolutePath}") }
                .delete()
        }.onFailure { t -> println("Failed to delete $portFile: $t") }

        runner?.let {
            println("Stopping DynamoDB local instance on port $port")
            it.stop()
        }
    }
}

val startDdbLocal = task<DynamoDbLocalInstance>("startDdbLocal") {
    portFile.set(file("build/ddblocal/port.info")) // Keep in sync with DdbLocalTest.kt
    outputs.upToDateWhen { false } // Always run this task even if a portFile already exists
}

tasks.withType<Test> {
    dependsOn(startDdbLocal)
    doLast {
        startDdbLocal.stop()
    }
}
