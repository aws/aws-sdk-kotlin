/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.spike.customclient

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
public abstract class GenerateTask : DefaultTask() {
    @get:Input
    // Not `services`: that name clashes with AbstractTask.getServices().
    public abstract val clientServices: MapProperty<String, String>

    @get:Input
    public abstract val visibility: Property<String>

    @get:Input
    public abstract val paginators: Property<Boolean>

    @get:Classpath
    public abstract val codegenClasspath: ConfigurableFileCollection

    @get:Internal
    public abstract val index: Property<IndexService>

    /** Kotlin source root. Holds only `.kt` files; AGP may set this location itself. */
    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    /** Kept apart from [outputDir] so lock files never land in a source set. */
    @get:OutputDirectory
    public abstract val lockDir: DirectoryProperty

    @get:Inject
    public abstract val workers: WorkerExecutor

    @TaskAction
    public fun generate() {
        listOf(outputDir, lockDir).forEach { d -> d.get().asFile.apply { deleteRecursively(); mkdirs() } }

        val queue = workers.classLoaderIsolation { classpath.from(codegenClasspath) }
        clientServices.get().forEach { (service, operations) ->
            queue.submit(GenerateAction::class.java) {
                this.service.set(service)
                this.operations.set(operations.split(","))
                this.visibility.set(this@GenerateTask.visibility)
                this.outputDir.set(this@GenerateTask.outputDir)
                this.lockDir.set(this@GenerateTask.lockDir)
            }
        }
    }
}

public interface GenerateParameters : WorkParameters {
    public val service: Property<String>
    public val operations: ListProperty<String>
    public val visibility: Property<String>
    public val outputDir: DirectoryProperty
    public val lockDir: DirectoryProperty
}

public abstract class GenerateAction : WorkAction<GenerateParameters> {
    override fun execute() {
        val p = parameters
        val svc = p.service.get()
        val pkg = "aws.sdk.kotlin.services.${svc.lowercase()}"
        val vis = p.visibility.get()
        val dir = p.outputDir.get().asFile.resolve(pkg.replace('.', '/'))
        dir.mkdirs()
        val ops = p.operations.get().sorted()
        dir.resolve("${svc}Client.kt").writeText(
            buildString {
                appendLine("package $pkg")
                appendLine()
                appendLine("$vis class ${svc}Client {")
                ops.forEach { appendLine("    $vis fun $it(): String = \"$it\"") }
                appendLine("}")
            },
        )
        // Gson only exists on the isolated worker classpath, which proves classloader isolation works.
        val lock = mapOf("service" to svc, "operations" to ops, "visibility" to vis)
        p.lockDir.get().asFile.resolve("$svc.lock.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(lock))
    }
}

@CacheableTask
public abstract class LockReportTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val lockDirs: ConfigurableFileCollection

    @get:OutputFile
    public abstract val reportFile: RegularFileProperty

    @TaskAction
    public fun report() {
        val entries = lockDirs.files.flatMap { dir -> dir.listFiles { f -> f.name.endsWith(".lock.json") }.orEmpty().toList() }
            .sortedBy { it.name }
            .map { it.readText().trim() }
        reportFile.get().asFile.apply { parentFile.mkdirs() }.writeText(entries.joinToString(",\n", "[\n", "\n]\n"))
    }
}

public abstract class ConflictCheckTask : DefaultTask() {
    @get:Input
    public abstract val componentIds: SetProperty<String>

    @TaskAction
    public fun check() {
        val full = componentIds.get().filter { it.startsWith("aws.sdk.kotlin:s3:") }
        if (full.isNotEmpty()) logger.warn("w: custom S3 client conflicts with $full")
        logger.lifecycle("awsSdkKotlin: conflict check saw ${componentIds.get().size} components")
    }
}
