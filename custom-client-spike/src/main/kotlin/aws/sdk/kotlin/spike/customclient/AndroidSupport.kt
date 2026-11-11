/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.spike.customclient

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.plugins.ExtensionAware

/**
 * Configures the AWS SDK custom client for this Android source set. Needed for AGP 9 built-in Kotlin, which has no
 * `main` Kotlin source set.
 */
public fun AndroidSourceSet.awsSdkKotlin(block: ClientSpec.() -> Unit) {
    (this as ExtensionAware).extensions.getByType(ClientSpec::class.java).use(block)
}

/** Only loaded once an Android plugin is applied, so AGP stays a compile-only dependency. */
internal object AndroidSupport {
    fun configure(ctx: PluginContext) {
        val project = ctx.project
        val android = project.extensions.getByName("android")

        // Reflection: CommonExtension's type parameters differ between AGP 8 and 9.
        @Suppress("UNCHECKED_CAST")
        val sourceSets = android.javaClass.getMethod("getSourceSets").invoke(android) as NamedDomainObjectContainer<AndroidSourceSet>

        val specs = mutableMapOf<String, ClientSpec>()
        sourceSets.configureEach {
            val sourceSet = this
            val spec = ctx.newSpec(sourceSet as ExtensionAware)
            spec.activate = {
                specs[sourceSet.name] = spec
                val configName = if (sourceSet.name == "main") "implementation" else "${sourceSet.name}Implementation"
                ctx.addRuntimeDeps(configName, spec)
            }
        }

        // One generate task per variant. AGP owns the output location, and variants can't share one task output.
        // The build cache dedupes variants with identical inputs.
        val components = project.extensions.getByType(AndroidComponentsExtension::class.java)
        components.onVariants(components.selector().all()) { variant ->
            val spec = specs["main"] ?: return@onVariants
            val generate = ctx.registerGenerate(variant.name, spec, defaultOutput = false)
            variant.sources.kotlin?.addGeneratedSourceDirectory(generate, GenerateTask::outputDir)
        }
    }
}
