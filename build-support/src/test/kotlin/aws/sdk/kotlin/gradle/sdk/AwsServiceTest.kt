/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import aws.smithy.kotlin.runtime.testing.withTempDir
import kotlinx.coroutines.runBlocking
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.*

class AwsServiceTest {

    val modelContents = """
        ${"$"}version: "2"
        namespace gradle.test

        use aws.api#service
        use aws.protocols#awsJson1_0

        @service(sdkId: "Test Gradle")
        @awsJson1_0
        service TestService {
            operations: [],
            version: "1-alpha"
        }
    """.trimIndent()

    private data class TestResult(
        val model: File,
        val actual: AwsService?,
    )

    private val defaultPackageManifest = PackageManifest(
        listOf(
            PackageMetadata(
                "Test Gradle",
                // namespace and artifact name intentionally don't match the sdkId derivations to verify we pull from
                // the metadata rather than inferring again
                "aws.sdk.kotlin.services.testgradle2",
                "test-gradle",
                "AwsSdkKotlinTestGradle",
            ),
        ),
    )

    private fun testWith(
        tempDir: File,
        bootstrap: BootstrapConfig,
        manifest: PackageManifest = defaultPackageManifest,
    ): TestResult {
        val project = ProjectBuilder.builder()
            .build()
        project.extra.set("sdkVersion", "1.2.3")

        // NOTE: Model assembler requires the correct .json or .smithy extension for the file contents
        val model = tempDir.resolve("test-gradle.smithy")
        model.writeText(modelContents)

        val lambda = fileToService(project, bootstrap, manifest)
        val actual = lambda(model)
        return TestResult(model, actual)
    }

    @Test
    fun testFileToService() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val tests = listOf(
                BootstrapConfig.ALL,
                // filename
                BootstrapConfig("+test-gradle"),
                BootstrapConfig("test-gradle"),
                // artifact name
                BootstrapConfig("+testgradle"),
                BootstrapConfig("testgradle"),
                // protocol
                BootstrapConfig(null, "awsJson1_0"),
            )

            tests.forEach { bootstrap ->
                val result = testWith(tempDir, bootstrap)
                val expected = AwsService(
                    "gradle.test#TestService",
                    "aws.sdk.kotlin.services.testgradle2",
                    "1.2.3",
                    result.model,
                    "test-gradle",
                    "Test Gradle",
                    "1-alpha",
                    "test-gradle",
                    "The AWS SDK for Kotlin client for Test Gradle",
                )
                assertEquals(expected, result.actual)
            }
        }
    }

    @Test
    fun testFileToServiceExclude() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val tests = listOf(
                // explicit exclude
                BootstrapConfig("-test-gradle"),
                BootstrapConfig("-testgradle"),
                // explicit include without service under test
                BootstrapConfig("other"),
                // protocol exclude
                BootstrapConfig(null, "-awsJson1_0"),
            )

            tests.forEach { bootstrap ->
                val result = testWith(tempDir, bootstrap)
                assertNull(result.actual, "expected null for bootstrap with $bootstrap")
            }
        }
    }

    // FIXME - re-enable after migration
    // @Test
    // fun testFileToServiceMissingPackageMetadata() = runBlocking {
    //     withTempDir { dir ->
    //         val tempDir = File(dir.toString())
    //         val ex = assertFailsWith<IllegalStateException> {
    //             testWith(tempDir, BootstrapConfig.ALL, PackageManifest(emptyList()))
    //         }
    //         assertContains(ex.message!!, "unable to find package metadata for sdkId: Test Gradle")
    //     }
    // }
}
