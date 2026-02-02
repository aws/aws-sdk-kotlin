/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.dokka

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jsoup.Jsoup
import java.io.File
import javax.inject.Inject
import kotlin.io.walk

abstract class TrimNavigation @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    init {
        description = "Trims navigation.html files to remove unrelated projects' side menus"
        group = "documentation"
    }

    @TaskAction
    fun trimNavigation() {
        val queue = workerExecutor.noIsolation()
        val sourceDirectory = this.sourceDirectory.getAsFile().get()

        logger.info("Searching for navigation.html files in $sourceDirectory")
        sourceDirectory
            .walk()
            .filter { it.name == "navigation.html" && it.parentFile != sourceDirectory }
            .forEach { file ->
                queue.submit(TrimModule::class.java) {
                    navigationFile = file
                    projectRoot = project.layout.projectDirectory.asFile
                }
            }
    }
}

interface TrimModuleParameters : WorkParameters {
    var navigationFile: File
    var projectRoot: File
}

abstract class TrimModule : WorkAction<TrimModuleParameters> {
    override fun execute() {
        val navigation = parameters.navigationFile
        val moduleName = navigation.parentFile.name

        val relativePath = navigation.toRelativeString(parameters.projectRoot)
        val logger = Logging.getLogger(TrimNavigation::class.java)
        logger.info("Trimming $relativePath...")

        val doc = Jsoup.parse(navigation)

        // Remove all parent directory elements from all navigation links
        doc.select("a[href^=../]").forEach { anchor ->
            var href = anchor.attr("href")

            while (href.startsWith("../")) {
                href = href.removePrefix("../")
            }

            anchor.attr("href", href)
        }

        // Trim side menus
        doc.select("div.sideMenu > div.toc--part")
            .filterNot { it.id().startsWith("$moduleName-nav-submenu") }
            .forEach { moduleMenu ->
                val moduleRow = moduleMenu.select("div.toc--row").first()!!
                val toggleButton = moduleRow.select("button.toc--button").single()
                toggleButton.remove()

                moduleMenu.children()
                    .filterNot { it == moduleRow }
                    .forEach { it.remove() }
            }

        // Update navigation.html
        val trimmedSideMenuParts = doc.select("div.sideMenu > div.toc--part")
        navigation.writeText("<div class=\"sideMenu\">\n$trimmedSideMenuParts\n</div>")
    }
}
