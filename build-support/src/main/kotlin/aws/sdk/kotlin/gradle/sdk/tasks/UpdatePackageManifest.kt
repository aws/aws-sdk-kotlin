/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk.tasks

import aws.sdk.kotlin.gradle.sdk.PackageManifest
import aws.sdk.kotlin.gradle.sdk.PackageMetadata
import aws.sdk.kotlin.gradle.sdk.orNull
import aws.sdk.kotlin.gradle.sdk.validate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import kotlin.streams.toList

/**
 * Task to update the package manifest which is used by the bootstrap process to generate service clients.
 * New services are required to be scaffolded
 */
abstract class UpdatePackageManifest : DefaultTask() {

    @get:Option(option = "model", description = "the path to a single model file to scaffold")
    @get:Optional
    @get:InputFile
    public abstract val modelFile: RegularFileProperty

    @get:Optional
    @get:Option(option = "model-dir", description = "the path to a directory of model files to scaffold")
    @get:InputDirectory
    public abstract val modelDir: DirectoryProperty

    @get:Optional
    @get:Option(
        option = "discover",
        description = "Flag to discover and process only new packages not currently in the manifest. Only applicable when used in conjunction with `model-dir`",
    )
    @get:Input
    public abstract val discover: Property<Boolean>

    @TaskAction
    fun updatePackageManifest() {
        check(modelFile.isPresent != modelDir.isPresent) { "Exactly one of `model` or `model-dir` must be set" }

        val manifestFile = project.file("packages.json")

        val manifest = if (manifestFile.exists()) {
            val manifest = PackageManifest.fromFile(manifestFile)
            manifest.validate()
            manifest
        } else {
            PackageManifest(emptyList())
        }

        val model = Model.assembler()
            .discoverModels()
            .apply {
                val import = if (modelFile.isPresent) modelFile else modelDir
                addImport(import.get().asFile.absolutePath)
            }
            .assemble()
            .result
            .get()

        val discoveredPackages = model
            .shapes(ServiceShape::class.java)
            .toList()
            .mapNotNull { it.getTrait(ServiceTrait::class.java).orNull()?.sdkId }
            .map { PackageMetadata.from(it) }

        val newPackages = validatedPackages(manifest, discoveredPackages)

        if (newPackages.isEmpty()) {
            logger.lifecycle("no new packages to scaffold")
            return
        }

        logger.lifecycle("scaffolding ${newPackages.size} new service packages")

        val updatedPackages = manifest.packages + newPackages
        val updatedManifest = manifest.copy(packages = updatedPackages.sortedBy { it.sdkId })

        val json = Json { prettyPrint = true }
        val contents = json.encodeToString(updatedManifest)
        manifestFile.writeText(contents)
    }

    private fun validatedPackages(manifest: PackageManifest, discovered: List<PackageMetadata>): List<PackageMetadata> = if (modelDir.isPresent && discover.orNull == true) {
        val bySdkId = manifest.packages.associateBy(PackageMetadata::sdkId)
        discovered.filter { it.sdkId !in bySdkId }
    } else {
        discovered.forEach { pkg ->
            val existing = manifest.packages.find { it.sdkId == pkg.sdkId }
            check(existing == null) { "found existing package in manifest for sdkId `${pkg.sdkId}`: $existing" }
        }
        discovered
    }
}
