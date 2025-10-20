package aws.sdk.kotlin.dokka

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jsoup.Jsoup
import java.io.File
import javax.inject.Inject

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

        sourceDirectory
            .asFileTree
            .filter { it.isDirectory() && it.resolve("navigation.html").exists() }
            .forEach { dir ->
                queue.submit(TrimModule::class.java) {
                    moduleDirectory = dir
                    logger = this@TrimNavigation.logger
                    projectRoot = project.layout.projectDirectory.asFile
                }
            }
    }
}

interface TrimModuleParameters : WorkParameters {
    var logger: Logger
    var moduleDirectory: File
    var projectRoot: File
}

abstract class TrimModule : WorkAction<TrimModuleParameters> {
    override fun execute() {
        val moduleDirectory = parameters.moduleDirectory
        val moduleName = moduleDirectory.name
        val navigation = moduleDirectory.resolve("navigation.html")

        val logger = parameters.logger
        val relativePath = navigation.toRelativeString(parameters.projectRoot)
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