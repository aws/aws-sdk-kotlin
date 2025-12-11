/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.gradle.codegen.*
import aws.sdk.kotlin.gradle.codegen.dsl.SmithyProjection
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.sdk.*
import aws.sdk.kotlin.gradle.sdk.tasks.UpdatePackageManifest
import aws.sdk.kotlin.gradle.util.typedProp
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import java.nio.file.Paths
import kotlin.streams.toList

plugins {
    kotlin("jvm")
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
    id("sdk-bootstrap")
}

val sdkVersion: String by project
description = "AWS SDK codegen tasks"

val servicesProvider: Provider<List<AwsService>> = project.provider { discoverServices() }

// Manually create the projections rather than using the extension to avoid unnecessary configuration evaluation.
// Otherwise we would be reading the models from disk on every gradle invocation for unrelated projects/tasks
fun awsServiceProjections(): Provider<List<SmithyProjection>> {
    logger.info("AWS service projection provider called")
    val p = servicesProvider.map {
        it.map { service ->
            SmithyProjection(
                service.projectionName,
            ).apply {
                val importPaths = mutableListOf(service.modelFile.absolutePath)
                if (file(service.modelExtrasDir).exists()) {
                    importPaths.add(service.modelExtrasDir)
                }
                imports = importPaths
                transforms = (transformsForService(service) ?: emptyList()) + REMOVE_DEPRECATED_SHAPES_TRANSFORM

                val packageSettings = PackageSettings.fromFile(service.sdkId, file(service.packageSettings))

                smithyKotlinPlugin {
                    serviceShapeId = service.serviceShapeId
                    packageName = service.packageName
                    packageVersion = service.packageVersion
                    packageDescription = service.description
                    sdkId = service.sdkId
                    buildSettings {
                        generateFullProject = false
                        generateDefaultBuildFiles = false
                    }
                    apiSettings {
                        enableEndpointAuthProvider = packageSettings.enableEndpointAuthProvider
                    }
                }
            }
        }
    }

    // get around class cast issues, listProperty implements what we need to pass this to `NamedObjectContainer`
    return p
}

// this will lazily evaluate the provider and only cause the models to be
// mapped if the tasks are actually needed
// NOTE: FYI evaluation still happens if you ask for the list of tasks or rebuild the gradle model in intellij
smithyBuild.projections.addAllLater(awsServiceProjections())

/**
 * This function retrieves Smithy transforms associated with the target service to be merged into generated
 * `smithy-build.json` files.  The transform file MUST live in <service dir>/transforms.
 * The JSON fragment MUST be a JSON object of a transform as described on this page:
 * https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#transforms.
 *
 * Transform filenames should follow the convention of:
 * <transform operation>-<target shape(s)>.json
 * Example: renameShapes-MarketplaceCommerceAnalyticsException.json
 */
fun transformsForService(service: AwsService): List<String>? {
    val transformsDir = File(service.transformsDir)
    return transformsDir.listFiles()?.map { transformFile ->
        transformFile.readText()
    }
}

val bootstrap = BootstrapConfig(
    typedProp("aws.services"),
    typedProp("aws.protocols"),
)

/**
 * Returns an AwsService model for every JSON file found in directory defined by property `modelsDirProp`
 * @param applyFilters Flag indicating if models should be filtered to respect the `aws.services` and `aws.protocol`
 * membership tests
 */
fun discoverServices(applyFilters: Boolean = true): List<AwsService> {
    logger.info("discover services called")
    val modelsDir: String by project
    val bootstrapConfig = bootstrap.takeIf { applyFilters } ?: BootstrapConfig.ALL
    val pkgManifest = PackageManifest
        .fromFile(file("packages.json"))
        .apply {
            validate()
        }

    return fileTree(project.file(modelsDir)).mapNotNull(fileToService(project, bootstrapConfig, pkgManifest)).also {
        logger.lifecycle("discovered ${it.size} services")
    }
}

/**
 * The project directory under `aws-sdk-kotlin/services`
 *
 * NOTE: this will also be the artifact name in the GAV coordinates
 */
val AwsService.destinationDir: String
    get() = rootProject.file("services/$artifactName").absolutePath

/**
 * Service specific model extras
 */
val AwsService.modelExtrasDir: String
    get() = rootProject.file("$destinationDir/model").absolutePath

/**
 * Defines where service-specific transforms are located
 */
val AwsService.transformsDir: String
    get() = rootProject.file("$destinationDir/transforms").absolutePath

/**
 * Service specific package settings
 */
val AwsService.packageSettings: String
    get() = rootProject.file("$destinationDir/package.json").absolutePath

fun forwardProperty(name: String) {
    typedProp<String>(name)?.let {
        System.setProperty(name, it)
    }
}

val codegen by configurations.getting
dependencies {
    codegen(project(":codegen:aws-sdk-codegen"))
    codegen(libs.smithy.cli)
    codegen(libs.smithy.model)
    codegen(libs.smithy.smoke.test.traits)
    codegen(libs.smithy.aws.smoke.test.model)
}

tasks.generateSmithyProjections {
    inputs.property("bootstrapConfigHash", bootstrap.hashCode())
    doFirst {
        forwardProperty("aws.partitions_file")
        forwardProperty("aws.user_agent.add_metadata")
    }
}

val stageSdkTasks = servicesProvider.map { discoveredServices ->
    logger.lifecycle("discoveredServices = ${discoveredServices.joinToString { it.sdkId }}")
    discoveredServices.map { service ->
        tasks.register<Copy>("stageSdk-${service.projectionName}") {
            val projectionOutputDir = smithyBuild.smithyKotlinProjectionPath(service.projectionName).get()

            description = "Copy generated files for ${service.sdkId} to services directory"
            group = "codegen"
            dependsOn(tasks.generateSmithyProjections)

            doFirst { logger.lifecycle("Copying $projectionOutputDir to ${service.destinationDir}") }

            // Copy src directory
            from("$projectionOutputDir/src")
            into("${service.destinationDir}/generated-src")

            // Copy build file
            from("$projectionOutputDir/build.gradle.kts") {
                into("..")
            }

            // Copy overview
            from("$projectionOutputDir/OVERVIEW.md") {
                into("..")
            }
        }
    }
}

val stageSdks = tasks.register("stageSdks") {
    group = "codegen"
    description = "Relocate generated SDK(s) from build directory to services/ dir"
    dependsOn(stageSdkTasks)
}

tasks.register("bootstrap") {
    group = "codegen"
    description = "Generate AWS SDK's and register them with the build"

    dependsOn(tasks.generateSmithyProjections)
    finalizedBy(stageSdks)
}

tasks.register<UpdatePackageManifest>("updatePackageManifest") {
    group = "codegen"
    description = "Add (or update) one or more services to packages.json manifest"
}

/**
 * Represents a type for a model that is sourced from aws-models
 */
data class SourceModel(
    /**
     * The path in aws-models to the model file
     */
    val path: String,
    /**
     * The sdkId trait value from the model
     */
    val sdkId: String,
    /**
     * The service version from the model
     */
    val version: String,
) {
    /**
     * The model filename in aws-sdk-kotlin
     */
    val destFilename: String
        get() = "${sdkIdToModelFilename(sdkId)}.json"
}

fun discoverSourceModels(repoPath: String): List<SourceModel> {
    val root = File(repoPath)
    val models = root.listFiles()
        ?.map { Paths.get(it.absolutePath, "smithy", "model.json").toFile() }
        ?.filter { it.exists() } ?: error("no models found in $root")

    return models.map { file ->
        val model = Model.assembler().addImport(file.absolutePath).assemble().result.get()
        val services: List<ServiceShape> = model.shapes(ServiceShape::class.java).sorted().toList()
        require(services.size == 1) { "Expected one service per aws model, but found ${services.size} in ${file.absolutePath}: ${services.map { it.id }}" }
        val service = services.first()
        val serviceApi = service.getTrait(software.amazon.smithy.aws.traits.ServiceTrait::class.java).orNull()
            ?: error("Expected aws.api#service trait attached to model ${file.absolutePath}")

        SourceModel(file.absolutePath, serviceApi.sdkId, service.version)
    }
}

fun discoverAwsModelsRepoPath(): String? {
    val discovered = rootProject.file("../aws-models")
    if (discovered.exists()) return discovered.absolutePath

    return typedProp<String>("awsModelsDir")?.let { File(it) }?.absolutePath
}

/**
 * Synchronize upstream changes from aws-models repository.
 *
 * Steps to synchronize:
 * 1. Clone aws-models if not already cloned
 * 2. `cd <path/to/aws-models>`
 * 3. `git pull` to pull down the latest changes
 * 4. `cd <path/to/aws-sdk-kotlin>`
 * 5. Run `./gradlew syncAwsModels`
 * 6. Check in any new models as needed (view warnings generated at end of output)
 */
tasks.register("syncAwsModels") {
    group = "codegen"
    description = "Sync upstream changes from aws-models repo"

    doLast {
        println("syncing AWS models")
        val repoPath = discoverAwsModelsRepoPath() ?: error("Failed to discover path to aws-models. Explicitly set -PawsModelsDir=<path-to-local-repo>")
        val sourceModelsBySdkId = discoverSourceModels(repoPath).associateBy { it.sdkId }

        val existingModelsBySdkId = discoverServices(applyFilters = false).associateBy { it.sdkId }

        val existingSdkIdSet = existingModelsBySdkId.values.map { it.sdkId }.toSet()
        val sourceSdkIdSet = sourceModelsBySdkId.values.map { it.sdkId }.toSet()

        // sync known existing models
        val pairs = existingModelsBySdkId.values.mapNotNull { existing ->
            sourceModelsBySdkId[existing.sdkId]?.let { source -> Pair(source, existing) }
        }

        val modelsDir = project.file("aws-models")

        pairs.forEach { (source, existing) ->
            // ensure we don't accidentally take a new API version
            if (source.version != existing.version) error("upstream version of ${source.path} does not match destination of existing model version ${existing.modelFile.name}")

            println("syncing ${existing.modelFile.name} from ${source.path}")
            copy {
                from(source.path)
                into(modelsDir)
                rename { existing.modelFile.name }
            }
        }

        val orphaned = existingSdkIdSet - sourceSdkIdSet
        val newSources = sourceSdkIdSet - existingSdkIdSet

        // sync new models
        newSources.forEach { sdkId ->
            val source = sourceModelsBySdkId[sdkId]!!
            val dest = Paths.get(modelsDir.absolutePath, source.destFilename).toFile()
            println("syncing new model ${source.sdkId} to $dest")
            copy {
                from(source.path)
                into(modelsDir)
                rename { source.destFilename }
            }
        }

        // generate warnings at the end so they are more visible
        if (orphaned.isNotEmpty() || newSources.isNotEmpty()) {
            println("\nWarnings:")
        }

        // models with no upstream source in aws-models
        orphaned.forEach { sdkId ->
            val existing = existingModelsBySdkId[sdkId]!!
            logger.warn("Cannot find a model file for ${existing.modelFile.name} (sdkId=${existing.sdkId}) but it exists in aws-sdk-kotlin!")
        }

        // new since last sync
        newSources.forEach { sdkId ->
            val source = sourceModelsBySdkId[sdkId]!!
            logger.warn("${source.path} (sdkId=$sdkId) is new to aws-models since the last sync!")
        }
    }
}
