/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import java.io.File
import java.net.URL
import java.net.URLClassLoader

public class SchemaGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        val extension = createExtension()
        configureDependencies()

        project.afterEvaluate {
            extensions.configure<KspExtension> {
                arg(AnnotationsProcessorOptions.GenerateBuilderClassesAttribute.name, extension.generateBuilderClasses.name)
                arg(RenderOptions.VisibilityAttribute.name, extension.visibility.name)
                arg(AnnotationsProcessorOptions.DestinationPackageAttribute.name, extension.destinationPackage.toString())
                arg(AnnotationsProcessorOptions.GenerateGetTableMethodAttribute.name, extension.generateGetTableExtension.toString())
            }
        }
    }

    private fun Project.createExtension(): SchemaGeneratorPluginExtension = extensions.create<SchemaGeneratorPluginExtension>(SCHEMA_GENERATOR_PLUGIN_EXTENSION)

    private fun Project.configureDependencies() {
        logger.info("Configuring dependencies for schema generation...")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
        }

        when (PROCESSOR_CLASSPATH_MODE) {
            "provided" -> {
                // The surrounding build environment has already placed the schema-codegen processor and its full
                // dependency closure on this plugin's runtime classpath. Inject those files directly instead of
                // declaring a coordinate, since no repository able to resolve the coordinate/closure is available.
                dependencies.add("ksp", files(resolveProvidedProcessorClasspath()))
            }
            else -> {
                // Declare a coordinate for the processor and let the consumer's build resolve it from a repository.
                val sdkVersion = getSdkVersion()
                dependencies.add("ksp", "aws.sdk.kotlin:dynamodb-mapper-schema-codegen:$sdkVersion")
            }
        }
    }

    // Reads sdk-version.txt for the SDK version to add dependencies on. The file is created in this module's build.gradle.kts
    private fun getSdkVersion(): String = checkNotNull(this::class.java.getResource("sdk-version.txt")?.readText()) { "Could not read sdk-version.txt" }

    /**
     * Collects the schema-codegen KSP processor and its dependency closure from this plugin's own runtime
     * classpath. Used when [PROCESSOR_CLASSPATH_MODE] is `provided`: the surrounding build environment supplies
     * these files on the plugin class loader, so they can be added to the `ksp` configuration directly without
     * repository resolution.
     */
    private fun resolveProvidedProcessorClasspath(): List<File> {
        val classLoader = javaClass.classLoader
        val urls = (classLoader as? URLClassLoader)?.urLs
            ?: error(
                "Cannot supply the schema-generation processor classpath: the plugin class loader " +
                    "(${classLoader::class.java.name}) does not expose its URLs. The `provided` processor classpath " +
                    "mode requires an environment that places the processor on the plugin's runtime classpath.",
            )

        val candidates = urls.mapNotNull { it.toFileOrNull() }
        val selfLocation = SchemaGeneratorPlugin::class.java.protectionDomain?.codeSource?.location?.toFileOrNull()

        // Anchor on a class that only exists in the schema-codegen artifact, to guarantee the processor's own jar
        // is present even if the class loader layout is unusual.
        val anchor = runCatching {
            Class.forName("aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorProvider")
                .protectionDomain?.codeSource?.location
        }.getOrNull()?.toFileOrNull()

        return selectProvidedProcessorClasspath(candidates, selfLocation, anchor)
    }
}

private fun URL.toFileOrNull(): File? = runCatching { File(toURI()) }.getOrNull()

/**
 * Artifacts present on the plugin's runtime classpath purely for Gradle wiring; they must not leak onto the KSP
 * processor classpath, where they are unnecessary and can conflict with the Kotlin compiler.
 */
private val PROCESSOR_CLASSPATH_EXCLUSIONS = listOf(
    "kotlin-gradle-plugin",
    "symbol-processing-gradle-plugin",
    "gradle-api",
    "gradle-kotlin-dsl",
    "kotlin-compiler-embeddable",
)

/**
 * Selects the KSP processor classpath from [candidates] (the plugin's own runtime classpath): drops the plugin's
 * own artifact ([selfLocation]) and the Gradle/KGP/KSP-Gradle wiring artifacts in [PROCESSOR_CLASSPATH_EXCLUSIONS],
 * and guarantees the processor's own artifact ([anchor]) is present.
 *
 * Kept pure and side-effect free so it can be unit tested without a Gradle project or class loader.
 */
internal fun selectProvidedProcessorClasspath(
    candidates: List<File>,
    selfLocation: File?,
    anchor: File?,
): List<File> {
    val files = candidates
        .asSequence()
        .filter { it != selfLocation }
        .filter { file -> PROCESSOR_CLASSPATH_EXCLUSIONS.none { file.name.contains(it, ignoreCase = true) } }
        .toMutableList()

    if (anchor != null && anchor !in files) files.add(anchor)

    check(files.isNotEmpty()) {
        "Resolved an empty schema-generation processor classpath from the plugin runtime classpath."
    }
    return files
}
