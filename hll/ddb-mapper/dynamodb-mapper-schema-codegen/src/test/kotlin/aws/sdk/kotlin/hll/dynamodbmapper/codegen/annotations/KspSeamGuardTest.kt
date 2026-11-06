/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the extraction/rendering seam: the IR (`model/`) and renderers (`rendering/`) must never depend on KSP. Only
 * `extraction/` and the processor may. Gradle runs tests from the project directory, so sources are read relative to it.
 */
class KspSeamGuardTest {
    private val packageRoot = File("src/main/kotlin/aws/sdk/kotlin/hll/dynamodbmapper/codegen/annotations")

    // Matches imports and fully-qualified inline references alike
    private val kspImport = Regex("""\b(com\.google\.devtools\.ksp|aws\.sdk\.kotlin\.hll\.codegen\.ksp)\b""")

    private fun kspImportsUnder(dir: String): List<String> {
        val root = File(packageRoot, dir)
        assertTrue(root.isDirectory, "Expected source directory ${root.absolutePath}")
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> kspImport.findAll(file.readText()).map { "${file.relativeTo(root)}: ${it.value}" } }
            .toList()
    }

    @Test
    fun testGuardDetectsKspImports() {
        val sample = "package x\n\nimport aws.sdk.kotlin.hll.codegen.model.Type\nimport com.google.devtools.ksp.symbol.KSType\n" +
            "import aws.sdk.kotlin.hll.codegen.ksp.asSymbolRef\nfun f(t: com.google.devtools.ksp.symbol.KSNode) = Unit\n"
        assertEquals(3, kspImport.findAll(sample).count())
    }

    @Test
    fun testModelIsKspFree() {
        assertEquals(emptyList(), kspImportsUnder("model"))
    }

    @Test
    fun testRenderingIsKspFree() {
        assertEquals(emptyList(), kspImportsUnder("rendering"))
    }
}
