/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.hll.configureKspCodegen
import dev.mokkery.verify.VerifyMode
import software.amazon.dynamodb.services.local.main.ServerRunner
import software.amazon.dynamodb.services.local.server.DynamoDBProxyServer
import java.net.ServerSocket

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
    alias(libs.plugins.mokkery)
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
                implementation(libs.smithy.kotlin.testing)
            }
        }
    }
}

ksp {
    arg("pkg", "aws.sdk.kotlin.hll.dynamodbmapper.operations")

    val allowlist = listOf(
        "batchGetItem",
        "batchWriteItem",
        "deleteItem",
        "getItem",
        "putItem",
        "query",
        "scan",
        "transactGetItems",
        "transactWriteItems",
        "updateItem",
    )
    arg("op-allowlist", allowlist.joinToString(";"))
}

configureKspCodegen(listOf(":hll:ddb-mapper:dynamodb-mapper-ops-codegen"))

/**
 * Owns a single DynamoDB Local instance shared by every test task in the build (e.g., `jvmTest` and `linuxX64Test`).
 * Gradle closes the service once the build finishes, so the server outlives whichever test task happens to finish
 * first instead of being torn down by it.
 */
abstract class DynamoDbLocalService :
    BuildService<DynamoDbLocalService.Params>,
    AutoCloseable {
    interface Params : BuildServiceParameters {
        val portFile: RegularFileProperty
    }

    private var runner: DynamoDBProxyServer? = null
    private var port: Int? = null

    @Synchronized
    fun start() {
        if (runner != null) return

        val port = ServerSocket(0).use { it.localPort }
        println("Starting DynamoDB local instance on port $port")
        val server = ServerRunner
            .createServerFromCommandLineArgs(arrayOf("-inMemory", "-port", port.toString(), "-disableTelemetry"))
        server.start()
        runner = server
        this.port = port

        parameters
            .portFile
            .asFile
            .get()
            .also { println("Writing port info file to ${it.absolutePath}") }
            .apply { parentFile.mkdirs() }
            .writeText(port.toString())
    }

    @Synchronized
    override fun close() {
        runCatching {
            parameters
                .portFile
                .asFile
                .get()
                .also { println("Deleting port info file at ${it.absolutePath}") }
                .delete()
        }.onFailure { t -> println("Failed to delete ${parameters.portFile}: $t") }

        runner?.let {
            println("Stopping DynamoDB local instance on port $port")
            it.stop()
        }
        runner = null
    }
}

val ddbLocalService = gradle.sharedServices.registerIfAbsent("ddbLocal-${project.path}", DynamoDbLocalService::class) {
    parameters.portFile.set(file("build/ddblocal/port.info")) // Keep in sync with DdbLocalTest.kt
}

abstract class StartDynamoDbLocal : DefaultTask() {
    @get:Internal
    abstract val ddbLocal: Property<DynamoDbLocalService>

    @TaskAction
    fun exec() = ddbLocal.get().start()
}

// No declared outputs, so Gradle always runs this task rather than treating it as up to date
val startDdbLocal = tasks.register<StartDynamoDbLocal>("startDdbLocal") {
    ddbLocal.set(ddbLocalService)
    usesService(ddbLocalService)
}

tasks.withType<AbstractTestTask> {
    dependsOn(startDdbLocal)
    // Keeps the service (and therefore the server) alive until every test task using it has finished
    usesService(ddbLocalService)
}

mokkery {
    defaultVerifyMode.set(VerifyMode.order)
}
