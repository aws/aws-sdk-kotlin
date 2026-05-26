/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.dokka

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

abstract class TrimNavigation : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    init {
        description = "Trims navigation.html files to remove unrelated projects' side menus"
        group = "documentation"
    }

    @TaskAction
    fun trimNavigation() {
        val sourceDir = sourceDirectory.asFile.get()
        val rootNavFile = sourceDir.resolve("navigation.html")

        if (!rootNavFile.exists()) {
            logger.warn("Root navigation.html not found at $rootNavFile, skipping trim")
            return
        }

        val rootNavSizeMb = rootNavFile.length().toDouble() / 1024 / 1024
        logger.lifecycle("[TrimNavigation] Parsing root navigation.html (${"%.3f".format(rootNavSizeMb)} MB)...")
        val (rootDoc, parseTime) = measureTimedValue { Jsoup.parse(rootNavFile) }
        logger.lifecycle("[TrimNavigation] Parsed in $parseTime")

        rootDoc.select("a[href^=../]").forEach { anchor ->
            var href = anchor.attr("href")
            while (href.startsWith("../")) {
                href = href.removePrefix("../")
            }
            anchor.attr("href", href)
        }

        val tocParts = rootDoc.select("div.sideMenu > div.toc--part")

        data class Segment(val id: String, val collapsed: ByteArray, val full: ByteArray)

        val segments = tocParts.map { part ->
            val id = part.id()
            val moduleRow = part.select("div.toc--row").first()!!
            val collapsedPart = (part.shallowClone() as Element).apply {
                appendChild(
                    moduleRow.clone().apply {
                        select("button.toc--button").remove()
                    },
                )
            }
            Segment(id, collapsedPart.outerHtml().toByteArray(), part.outerHtml().toByteArray())
        }

        val moduleNavFiles: List<Pair<String, File>> = sourceDir.walk()
            .filter { it.name == "navigation.html" && it.parentFile != sourceDir }
            .map { it.parentFile.name to it }
            .toList()

        val totalSegmentMb = segments.sumOf { it.collapsed.size.toLong() + it.full.size.toLong() }.toDouble() / 1024 / 1024
        logger.lifecycle("[TrimNavigation] ${segments.size} segments pre-extracted (${"%.3f".format(totalSegmentMb)} MB in memory)")
        logger.lifecycle("[TrimNavigation] Writing ${moduleNavFiles.size} trimmed navigation files...")

        val header = "<div class=\"sideMenu\">\n".toByteArray()
        val footer = "</div>".toByteArray()
        val newline = "\n".toByteArray()

        val writeTime = measureTime {
            moduleNavFiles.parallelStream().forEach { (moduleName, navFile) ->
                navFile.outputStream().buffered().use { out ->
                    out.write(header)
                    for (seg in segments) {
                        if (seg.id.startsWith("$moduleName-nav-submenu")) {
                            out.write(seg.full)
                        } else {
                            out.write(seg.collapsed)
                        }
                        out.write(newline)
                    }
                    out.write(footer)
                }
            }
        }

        logger.lifecycle("[TrimNavigation] Done trimming navigation files in $writeTime")
    }
}
