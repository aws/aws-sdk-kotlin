/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
    alias(libs.plugins.kotlinx.serialization)
}

data class BenchmarkProjection(val name: String, val serviceShapeId: String, val sdkId: String)

val benchmarkProjections = listOf(
    BenchmarkProjection("aws-rest-json", "smithy.benchmark.serde#AwsRestJsonDataPlane", "RestJsonDataPlane"),
    BenchmarkProjection("aws-json-rpc-1-0", "smithy.benchmark.serde#AwsJsonRpc10DataPlane", "JsonRpc10DataPlane"),
    BenchmarkProjection("smithy-rpc-v2-cbor", "smithy.benchmark.serde#SmithyRpcV2CborDataPlane", "RpcCborDataPlane"),
    BenchmarkProjection("aws-rest-xml", "smithy.benchmark.serde#AwsRestXmlDataPlane", "RestXmlDataPlane"),
    BenchmarkProjection("aws-query", "smithy.benchmark.serde#AwsQueryDataPlane", "QueryDataPlane"),
)

smithyBuild {
    benchmarkProjections.forEach { proj ->
        projections.register(proj.name) {
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

    // NOTE: The benchmark models are published to maven as a jar, this ensures that the benchmark models
    // dependency is found when generating code such that the `includeServices` transform actually works
    // TODO: uncomment once Smithy publishes the serde benchmark models, along with the version catalog entry
    //       and the `include(":benchmarks:serde-benchmarks")` in settings.gradle.kts
    // codegen(libs.smithy.serde.benchmark.models)

    implementation(libs.kotlinx.serialization.json)
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
    implementation(libs.smithy.kotlin.smithy.test)
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

// Stage generated sources (main + test/benchmark) into a single source set
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

        // Generate protocol registration map
        val imports = benchmarkProjections.joinToString("\n") {
            val pkg = it.name.replace("-", "")
            "import aws.sdk.kotlin.benchmarks.serde.$pkg.registerBenchmarks as register${it.sdkId}"
        }
        val mapEntries = benchmarkProjections.joinToString("\n") {
            "    \"${it.name}\" to ::register${it.sdkId},"
        }

        val outDir = layout.buildDirectory.dir("generated-src/main/aws/sdk/kotlin/benchmarks/serde").get().asFile
        outDir.mkdirs()
        outDir.resolve("ProtocolRegistrations.kt").writeText(
            """
            |package aws.sdk.kotlin.benchmarks.serde
            |
            |$imports
            |
            |internal val protocolRegistrations: Map<String, () -> Unit> = mapOf(
            |$mapEntries
            |)
            """.trimMargin(),
        )
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

val asyncProfilerLib: String? = providers.gradleProperty("asyncProfiler.libPath").orNull

fun JavaExec.configureBenchmarkTask() {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("aws.sdk.kotlin.benchmarks.serde.BenchmarkRunnerKt")
    systemProperty("benchmark.warmupSeconds", project.findProperty("benchmark.warmupSeconds") ?: "10")
    systemProperty("benchmark.measurementSeconds", project.findProperty("benchmark.measurementSeconds") ?: "30")
    systemProperty("benchmark.minIterations", project.findProperty("benchmark.minIterations") ?: "1000")
    systemProperty("benchmark.maxIterations", project.findProperty("benchmark.maxIterations") ?: "10000000")
    systemProperty("benchmark.instance", project.findProperty("benchmark.instance") ?: "unknown")
    systemProperty("smithy.kotlin.version", libs.versions.smithy.kotlin.version.get())
    systemProperty("aws.sdk.kotlin.version", project.findProperty("sdkVersion") ?: "SNAPSHOT")

    if (asyncProfilerLib != null) {
        val profilesDir = project.layout.buildDirectory.dir("profiles")
        doFirst { profilesDir.get().asFile.mkdirs() }
        jvmArgs("-agentpath:$asyncProfilerLib=start,event=cpu,file=${profilesDir.get()}/serde-profile.jfr")
    }
}

tasks.register<JavaExec>("runAllBenchmarks") {
    group = "benchmark"
    description = "Run all serde benchmarks sequentially"
    configureBenchmarkTask()
}

// Per-projection entry points, mirroring the `testProtocol-<projection>` tasks of :codegen:protocol-tests
benchmarkProjections.forEach { proj ->
    tasks.register<JavaExec>("benchmarkProtocol-${proj.name}") {
        group = "benchmark"
        description = "Run serde benchmarks for the ${proj.name} projection"
        configureBenchmarkTask()
        systemProperty("benchmark.protocol", proj.name)
    }
}

// Unlike `testAllProtocols`, this does not fan out to the per-projection tasks: all protocols are measured in a
// single JVM so the results share one warmup regime and land in one report.
tasks.register("benchmarkAllProtocols") {
    group = "benchmark"
    description = "Run the serde benchmarks for every projection"
    dependsOn("runAllBenchmarks")
}

// Codegen-only verification: proves every projection generates code that compiles. Cheap enough for CI.
tasks.register("verifyAllProtocols") {
    group = "verification"
    description = "Generate and compile the benchmark clients for every projection without running them"
    dependsOn(tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>())
}

tasks.register<JavaExec>("runBenchmark") {
    group = "benchmark"
    description = "Run serde benchmarks for specific protocol(s). Use -Pbenchmark.protocol=aws-rest-json (comma-separated for multiple)"
    configureBenchmarkTask()
    val availableProtocols = benchmarkProjections.map { it.name }
    val requestedProtocols = providers.gradleProperty("benchmark.protocol")

    // validated at execution time so that realizing this task (e.g. `gradlew tasks`, or any broad task graph in
    // CI) doesn't fail just because the property is absent
    doFirst {
        val protocol = requestedProtocols.orNull
            ?: error("benchmark.protocol property is required. Available: ${availableProtocols.joinToString()}")
        val requested = protocol.split(",").map { it.trim() }
        val invalid = requested.filter { req -> availableProtocols.none { it.contains(req, ignoreCase = true) } }
        if (invalid.isNotEmpty()) {
            error("Unknown protocol(s): ${invalid.joinToString()}. Available: ${availableProtocols.joinToString()}")
        }
        systemProperty("benchmark.protocol", protocol)
    }
}
