/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("org.jetbrains.kotlinx.benchmark") version libs.versions.kotlinx.benchmark.version.get()
    id("org.jetbrains.kotlin.plugin.allopen") version libs.versions.kotlin.version.get()
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

data class BenchmarkProjection(val name: String, val serviceShapeId: String, val sdkId: String)

val benchmarkProjections = listOf(
    BenchmarkProjection("aws-rest-json", "com.amazonaws.sdk.benchmark#AwsRestJsonDataPlane", "RestJsonDataPlane"),
    BenchmarkProjection("aws-json-rpc-1-0", "com.amazonaws.sdk.benchmark#AwsJsonRpc10DataPlane", "JsonRpc10DataPlane"),
    BenchmarkProjection("smithy-rpc-v2-cbor", "com.amazonaws.sdk.benchmark#SmithyRpcV2CborDataPlane", "RpcCborDataPlane"),
    BenchmarkProjection("aws-rest-xml", "com.amazonaws.sdk.benchmark#AwsRestXmlDataPlane", "RestXmlDataPlane"),
    BenchmarkProjection("aws-query", "com.amazonaws.sdk.benchmark#AwsQueryDataPlane", "QueryDataPlane"),
)

// Path to the AwsSdkPerformanceBenchmarkModels model directory
// Pass via: -PbenchmarkModelsDir=/path/to/AwsSdkPerformanceBenchmarkModels/.../model
val benchmarkModelsDir: String? = project.findProperty("benchmarkModelsDir") as? String

smithyBuild {
    benchmarkProjections.forEach { proj ->
        projections.register(proj.name) {
            imports = listOf(
                benchmarkModelsDir ?: error("benchmarkModelsDir property is required. Pass -PbenchmarkModelsDir=/path/to/model"),
            )

            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["${proj.serviceShapeId}"]
                  }
                }
                """,
            )

            smithyKotlinPlugin {
                serviceShapeId = proj.serviceShapeId
                packageName = "aws.sdk.kotlin.benchmarks.serde.${proj.name.replace("-", "")}"
                packageVersion = "1.0"
                sdkId = proj.sdkId
                buildSettings {
                    generateFullProject = false
                    generateDefaultBuildFiles = false
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
    codegen(libs.smithy.aws.protocol.tests)

    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":aws-runtime:aws-core"))
    implementation(project(":aws-runtime:aws-config"))
    implementation(project(":aws-runtime:aws-http"))
    implementation(project(":aws-runtime:aws-endpoint"))
    implementation(libs.smithy.kotlin.runtime.core)
    implementation(libs.smithy.kotlin.smithy.client)
    implementation(libs.smithy.kotlin.http)
    implementation(libs.smithy.kotlin.http.client)
    implementation(libs.smithy.kotlin.http.auth)
    implementation(libs.smithy.kotlin.http.auth.aws)
    implementation(libs.smithy.kotlin.http.client.engine.default)
    implementation(libs.smithy.kotlin.http.test)
    implementation(libs.smithy.kotlin.serde)
    implementation(libs.smithy.kotlin.serde.json)
    implementation(libs.smithy.kotlin.serde.cbor)
    implementation(libs.smithy.kotlin.serde.xml)
    implementation(libs.smithy.kotlin.serde.form.url)
    implementation(libs.smithy.kotlin.aws.credentials)
    implementation(libs.smithy.kotlin.aws.protocol.core)
    implementation(libs.smithy.kotlin.aws.json.protocols)
    implementation(libs.smithy.kotlin.aws.xml.protocols)
    implementation(libs.smithy.kotlin.aws.signing.common)
    implementation(libs.smithy.kotlin.aws.signing.default)
    implementation(libs.smithy.kotlin.identity.api)
    implementation(libs.smithy.kotlin.telemetry.defaults)
    implementation(libs.smithy.kotlin.smithy.rpcv2.protocols)
}

tasks.generateSmithyProjections {
    val sdkVersion: String by project
    doFirst {
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", sdkVersion)
    }
}

// Stage generated sources (main + test/benchmark) into a single source set for JMH
val stageGeneratedSources = tasks.register("stageGeneratedSources") {
    group = "codegen"
    dependsOn(tasks.generateSmithyProjections)
    doLast {
        benchmarkProjections.forEach { proj ->
            val projDir = smithyBuild.smithyKotlinProjectionPath(proj.name)
                .map { file(it.toString()) }.get()
            listOf("src/main/kotlin", "src/test/kotlin").forEach { sub ->
                val dir = projDir.resolve(sub)
                if (dir.exists()) {
                    copy {
                        from(dir)
                        into(layout.buildDirectory.dir("generated-src/main"))
                    }
                }
            }
        }
    }
}

sourceSets {
    main {
        kotlin.srcDir(layout.buildDirectory.dir("generated-src/main"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(stageGeneratedSources)
    compilerOptions {
        optIn.addAll(
            "aws.smithy.kotlin.runtime.InternalApi",
            "aws.sdk.kotlin.runtime.InternalSdkApi",
        )
    }
}

benchmark {
    targets {
        register("main")
    }
    configurations {
        named("main") {
            reportFormat = "json"
            iterations = 30
            warmups = 15
            iterationTime = 1
            iterationTimeUnit = "s"
            outputTimeUnit = "ns"
        }
    }
}

// Workaround for https://github.com/Kotlin/kotlinx-benchmark/issues/39
afterEvaluate {
    tasks.named<org.gradle.jvm.tasks.Jar>("mainBenchmarkJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    tasks.named<JavaExec>("mainBenchmark") {
        jvmArgs("-Xmx4g")
    }
}
