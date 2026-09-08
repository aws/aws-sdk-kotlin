/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Unit tests for [selectProvidedProcessorClasspath], the pure classpath-selection logic used by the plugin's
 * `provided` processor-classpath mode. These exercise the filtering rules directly, without a Gradle project or
 * class loader.
 */
class ProvidedProcessorClasspathTest {
    private fun jar(name: String) = File("/deps/$name")

    // Processor closure (should be kept)
    private val schemaCodegen = jar("dynamodb-mapper-schema-codegen-1.5.0.jar")
    private val hllCodegen = jar("hll-codegen-1.5.0.jar")
    private val mapper = jar("dynamodb-mapper-1.5.0.jar")
    private val smithyRuntime = jar("runtime-core-jvm-1.4.0.jar")

    // Gradle wiring / the plugin itself (should be dropped)
    private val pluginJar = jar("dynamodb-mapper-schema-generator-plugin-1.5.0.jar")
    private val kotlinGradlePlugin = jar("kotlin-gradle-plugin-2.0.20.jar")
    private val kspGradlePlugin = jar("symbol-processing-gradle-plugin-2.0.20-1.0.25.jar")
    private val gradleApi = jar("gradle-api-9.6.1.jar")
    private val gradleKotlinDsl = jar("gradle-kotlin-dsl-9.6.1.jar")
    private val kotlinCompiler = jar("kotlin-compiler-embeddable-2.0.20.jar")

    @Test
    fun keepsProcessorClosureAndDropsGradleWiring() {
        val candidates = listOf(
            schemaCodegen, hllCodegen, mapper, smithyRuntime,
            pluginJar, kotlinGradlePlugin, kspGradlePlugin, gradleApi, gradleKotlinDsl, kotlinCompiler,
        )

        val result = selectProvidedProcessorClasspath(candidates, selfLocation = pluginJar, anchor = schemaCodegen)

        // Processor closure retained
        assertContains(result, schemaCodegen)
        assertContains(result, hllCodegen)
        assertContains(result, mapper)
        assertContains(result, smithyRuntime)

        // Plugin's own jar and Gradle wiring removed
        assertFalse(pluginJar in result, "the plugin's own artifact must not be on the processor classpath")
        assertFalse(kotlinGradlePlugin in result)
        assertFalse(kspGradlePlugin in result)
        assertFalse(gradleApi in result)
        assertFalse(gradleKotlinDsl in result)
        assertFalse(kotlinCompiler in result)
    }

    @Test
    fun addsAnchorWhenAbsentFromCandidates() {
        val result = selectProvidedProcessorClasspath(
            candidates = listOf(hllCodegen, mapper),
            selfLocation = null,
            anchor = schemaCodegen,
        )
        assertContains(result, schemaCodegen)
    }

    @Test
    fun doesNotDuplicateAnchorWhenAlreadyPresent() {
        val result = selectProvidedProcessorClasspath(
            candidates = listOf(schemaCodegen, hllCodegen),
            selfLocation = null,
            anchor = schemaCodegen,
        )
        assertEquals(1, result.count { it == schemaCodegen })
    }

    @Test
    fun exclusionMatchingIsCaseInsensitive() {
        val mixedCaseKgp = jar("Kotlin-Gradle-Plugin-2.0.20.jar")
        val result = selectProvidedProcessorClasspath(
            candidates = listOf(schemaCodegen, mixedCaseKgp),
            selfLocation = null,
            anchor = null,
        )
        assertFalse(mixedCaseKgp in result)
        assertContains(result, schemaCodegen)
    }

    @Test
    fun throwsWhenNoCandidates() {
        assertFailsWith<IllegalStateException> {
            selectProvidedProcessorClasspath(candidates = emptyList(), selfLocation = null, anchor = null)
        }
    }

    @Test
    fun throwsWhenEverythingFilteredAndNoAnchor() {
        assertFailsWith<IllegalStateException> {
            selectProvidedProcessorClasspath(
                candidates = listOf(kotlinGradlePlugin, gradleApi),
                selfLocation = null,
                anchor = null,
            )
        }
    }
}
