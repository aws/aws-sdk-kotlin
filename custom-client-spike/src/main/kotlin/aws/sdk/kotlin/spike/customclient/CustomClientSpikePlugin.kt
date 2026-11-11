/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.spike.customclient

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal const val SPEC_EXTENSION = "awsSdkKotlinSpec"

/**
 * Configures the AWS SDK custom client for this Kotlin source set.
 */
public fun KotlinSourceSet.awsSdkKotlin(block: ClientSpec.() -> Unit) {
    (this as ExtensionAware).extensions.getByType(ClientSpec::class.java).use(block)
}

internal fun ClientSpec.use(block: ClientSpec.() -> Unit) {
    activate?.invoke()
    activate = null
    block()
}

public class CustomClientSpikePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val index = project.gradle.sharedServices.registerIfAbsent("awsSdkKotlinIndex", IndexService::class.java) { }

        val codegen = project.configurations.create("awsSdkKotlinCodegen") {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        project.dependencies.add(codegen.name, "com.google.code.gson:gson:2.11.0")

        val lock = project.tasks.register("awsSdkKotlinLock", LockReportTask::class.java) {
            reportFile.set(project.layout.buildDirectory.file("reports/aws-sdk-kotlin/lock.json"))
        }
        val ctx = PluginContext(project, codegen, index, lock)

        // Configure once, as soon as any plugin (KGP, or AGP's built-in Kotlin) registers the `kotlin` extension.
        var kotlinConfigured = false
        project.plugins.all {
            if (!kotlinConfigured && project.extensions.findByType(KotlinProjectExtension::class.java) != null) {
                kotlinConfigured = true
                configureKotlin(ctx)
            }
        }

        var androidConfigured = false
        listOf("com.android.application", "com.android.library").forEach { id ->
            project.pluginManager.withPlugin(id) {
                if (!androidConfigured) {
                    androidConfigured = true
                    // Lives in its own class: any AGP type referenced from this class breaks projects without AGP.
                    AndroidSupport.configure(ctx)
                }
            }
        }

        val check = project.tasks.register("awsSdkKotlinCheckConflicts", ConflictCheckTask::class.java) {
            componentIds.set(
                project.provider {
                    listOf("jvmRuntimeClasspath", "debugRuntimeClasspath", "runtimeClasspath")
                        .firstNotNullOfOrNull { project.configurations.findByName(it) }
                }.flatMap { cfg ->
                    cfg.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                        artifacts.map { it.id.componentIdentifier.displayName }.toSet()
                    }
                },
            )
        }
        project.pluginManager.withPlugin("base") {
            project.tasks.named("assemble") { dependsOn(lock) }
            project.tasks.named("check") { dependsOn(check) }
        }
    }

    private fun configureKotlin(ctx: PluginContext) {
        val kotlin = ctx.project.extensions.getByType(KotlinProjectExtension::class.java)
        kotlin.sourceSets.configureEach {
            val sourceSet = this
            val spec = ctx.newSpec(sourceSet as ExtensionAware)
            // Tasks and source dirs only exist for source sets the user actually configures.
            spec.activate = {
                val generate = ctx.registerGenerate(sourceSet.name, spec, defaultOutput = true)
                sourceSet.kotlin.srcDir(generate.flatMap { it.outputDir })
                ctx.addRuntimeDeps(sourceSet.implementationConfigurationName, spec)
            }
        }
    }
}

internal class PluginContext(
    val project: Project,
    val codegen: Configuration,
    val index: Provider<IndexService>,
    val lock: TaskProvider<LockReportTask>,
) {
    fun newSpec(owner: ExtensionAware): ClientSpec {
        val spec = owner.extensions.create(SPEC_EXTENSION, ClientSpec::class.java)
        spec.visibility.convention("internal")
        spec.generatePaginators.convention(true)
        spec.index.set(index)
        return spec
    }

    fun registerGenerate(name: String, spec: ClientSpec, defaultOutput: Boolean): TaskProvider<GenerateTask> {
        val cap = name.replaceFirstChar { it.uppercase() }
        val ctx = this
        val task = project.tasks.register("awsSdkKotlinGenerate$cap", GenerateTask::class.java) {
            clientServices.set(spec.services)
            visibility.set(spec.visibility)
            paginators.set(spec.generatePaginators)
            codegenClasspath.from(ctx.codegen)
            if (defaultOutput) outputDir.set(project.layout.buildDirectory.dir("generated/aws-sdk-kotlin/$name/kotlin"))
            lockDir.set(project.layout.buildDirectory.dir("aws-sdk-kotlin/lock/$name"))
            this.index.set(ctx.index)
            usesService(ctx.index)
        }
        lock.configure { lockDirs.from(task.flatMap { it.lockDir }) }
        return task
    }

    fun addRuntimeDeps(configurationName: String, spec: ClientSpec) {
        val deps = project.dependencies
        project.configurations.matching { it.name == configurationName }.configureEach {
            dependencies.addAllLater(
                spec.services.map { services ->
                    if (services.isEmpty()) {
                        emptyList()
                    } else {
                        val dep = deps.create("org.jetbrains.kotlinx:kotlinx-coroutines-core") as ExternalModuleDependency
                        dep.version { strictly("1.11.0") }
                        listOf(dep)
                    }
                },
            )
        }
    }
}
