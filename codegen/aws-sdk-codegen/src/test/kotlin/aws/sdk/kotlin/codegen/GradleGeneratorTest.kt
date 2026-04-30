/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.testutil.model
import aws.smithy.kotlin.codegen.core.KotlinDependency
import aws.smithy.kotlin.codegen.core.RUNTIME_VERSION
import aws.smithy.kotlin.codegen.rendering.writeGradleBuild
import aws.smithy.kotlin.codegen.test.defaultSettings
import aws.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import aws.smithy.kotlin.codegen.test.shouldNotContainWithDiff
import software.amazon.smithy.build.MockManifest
import kotlin.test.Test

class GradleGeneratorTest {
    @Test
    fun testRenderDependencies() {
        val model = model()
        val settings = model.defaultSettings()
        val manifest = MockManifest()
        val dependencies = listOf(KotlinDependency.KOTLIN_STDLIB, KotlinDependency.CORE)
        writeGradleBuild(settings, manifest, dependencies)

        val contents = manifest.getFileString("build.gradle.kts").get()

        contents.shouldContainOnlyOnceWithDiff("""api("aws.smithy.kotlin:runtime-core:$RUNTIME_VERSION")""")
        contents.shouldNotContainWithDiff("stdlib") // stdlib dependencies are implicit
    }
}
