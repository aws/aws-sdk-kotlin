/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.hll.configureKspCodegen
import dev.mokkery.verify.VerifyMode
import software.amazon.dynamodb.services.local.main.ServerRunner
import software.amazon.dynamodb.services.local.server.DynamoDBProxyServer
import java.net.ServerSocket
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

configureKspCodegen(listOf(":hll:dynamodb-mapper:dynamodb-mapper-ops-codegen"))

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

val startDdbLocal = tasks.register<DynamoDbLocalInstance>("startDdbLocal") {
    portFile.set(file("build/ddblocal/port.info")) // Keep in sync with DdbLocalTest.kt
    outputs.upToDateWhen { false } // Always run this task even if a portFile already exists
}

tasks.withType<Test> {
    dependsOn(startDdbLocal)
    doLast {
        startDdbLocal.get().stop()
    }
}

mokkery {
    defaultVerifyMode.set(VerifyMode.order)
}
