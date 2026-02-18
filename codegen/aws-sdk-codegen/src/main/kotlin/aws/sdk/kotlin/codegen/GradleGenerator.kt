/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.testing.TestFailedResponseTrait
import aws.sdk.kotlin.codegen.model.traits.testing.TestSuccessResponseTrait
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinDelegator
import aws.smithy.kotlin.codegen.core.KotlinDependency
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.hasTrait
import aws.smithy.kotlin.codegen.rendering.GradleWriter
import aws.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

// TODO - would be nice to allow integrations to define custom settings in the plugin
// e.g. we could then more consistently apply this integration if we could define a property like: `build.isAwsSdk: true`

/**
 * Integration that generates custom gradle build files
 */
class GradleGenerator : KotlinIntegration {

    // Specify to run last, to ensure all other integrations have had a chance to register dependencies.
    override val order: Byte
        get() = 127

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) = generateGradleBuild(ctx, delegator)

    /**
     * Generate a customized version of `build.gradle.kts` for AWS services
     *
     * Services belong to the `aws-sdk-kotlin` build which configures services via `subprojects {}`, thus the generated
     * gradle build needs to be stripped down.
     */
    private fun generateGradleBuild(ctx: CodegenContext, delegator: KotlinDelegator) {
        if (ctx.settings.build.generateDefaultBuildFiles) {
            // plugin settings are configured such that we should let smithy-kotlin generate the build
            // otherwise assume we are responsible
            return
        }

        val writer = GradleWriter()

        writer.write("")
        if (!ctx.settings.pkg.description.isNullOrEmpty()) {
            writer.write("description = #S", ctx.settings.pkg.description)
        }

        writer.write("project.ext.set(#S, #S)", "aws.sdk.id", ctx.settings.sdkId)
        writer.write("")

        val allDependencies = delegator.dependencies.mapNotNull { it.properties["dependency"] as? KotlinDependency }.distinct()

        writer
            .write("")
            .withBlock("kotlin {", "}") {
                withBlock("sourceSets {", "}") {
                    allDependencies
                        .sortedWith(compareBy({ it.config }, { it.artifact }))
                        .groupBy { it.config.sourceSet }
                        .forEach { (sourceSet, dependencies) ->
                            withBlock("$sourceSet {", "}") {
                                withBlock("dependencies {", "}") {
                                    dependencies
                                        .mapNotNull { it.dependencyNotation() }
                                        .forEach(::write)
                                }
                            }
                        }
                }
                if (ctx.model.topDownOperations(ctx.settings.service).any { it.hasTrait<SmokeTestsTrait>() }) {
                    write("")
                    generateSmokeTestConfig(writer, ctx)
                }
            }

        val contents = writer.toString()
        delegator.fileManifest.writeFile("build.gradle.kts", contents)
    }

    private fun generateSmokeTestConfig(writer: GradleWriter, ctx: CodegenContext) {
        generateSmokeTestJarTask(writer, ctx)
        writer.write("")
        generateSmokeTestTask(writer, ctx)
    }

    /**
     * Generates a gradle task to create smoke test runner JARs
     */
    private fun generateSmokeTestJarTask(writer: GradleWriter, ctx: CodegenContext) {
        writer.withBlock("jvm {", "}") {
            withBlock("compilations {", "}") {
                write("val mainPath = getByName(#S).output.classesDirs", "main")
                write("val testPath = getByName(#S).output.classesDirs", "test")
                withBlock("tasks {", "}") {
                    withBlock("register<Jar>(#S) {", "}", "smokeTestJar") {
                        write("description = #S", "Creates smoke tests jar")
                        write("group = #S", "application")
                        write("dependsOn(build)")
                        write("mustRunAfter(build)")
                        withBlock("manifest {", "}") {
                            write("attributes[#S] = #S", "Main-Class", "${ctx.settings.pkg.name}.smoketests.SmokeTestsKt")
                        }
                        write("val runtimePath = configurations.getByName(#S).map { if (it.isDirectory) it else zipTree(it) }", "jvmRuntimeClasspath")
                        write("duplicatesStrategy = DuplicatesStrategy.EXCLUDE")
                        write("from(runtimePath, mainPath, testPath)")
                        write("archiveBaseName.set(#S)", "\${project.name}-smoketests")
                    }
                }
            }
        }
    }

    /**
     * Generates a gradle task to run smoke tests
     */
    private fun generateSmokeTestTask(writer: GradleWriter, ctx: CodegenContext) {
        val hasSuccessResponseTrait = ctx.model.expectShape<ServiceShape>(ctx.settings.service).hasTrait(TestSuccessResponseTrait.ID)
        val hasFailedResponseTrait = ctx.model.expectShape<ServiceShape>(ctx.settings.service).hasTrait(TestFailedResponseTrait.ID)
        val inTestingEnvironment = hasFailedResponseTrait || hasSuccessResponseTrait

        /**
         * E2E tests don't have sdkVersion in jar names. They're added later for publishing.
         * @see SmokeTestE2ETest
         */
        val jarName = if (inTestingEnvironment) "\${project.name}-smoketests.jar" else "\${project.name}-smoketests-\$sdkVersion.jar"

        writer.withBlock("tasks.register<JavaExec>(#S) {", "}", "smokeTest") {
            write("description = #S", "Runs smoke tests jar")
            write("group = #S", "verification")
            write("dependsOn(tasks.getByName(#S))", "smokeTestJar")
            write("mustRunAfter(tasks.getByName(#S))", "smokeTestJar")
            write("")
            write("val sdkVersion: String by project")
            write("val jarFile = file(#S)", "build/libs/$jarName")
            write("classpath = files(jarFile)")
        }
    }
}
