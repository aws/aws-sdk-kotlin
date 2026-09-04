/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk.tasks

import aws.sdk.kotlin.gradle.sdk.PackageManifest
import aws.sdk.kotlin.gradle.sdk.PackageMetadata
import aws.smithy.kotlin.runtime.testing.withTempDir
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.*

class UpdatePackageManifestTaskTest {
    fun modelContents(sdkId: String, serviceName: String = "TestService"): String = """
        ${"$"}version: "2"
        namespace gradle.test

        use aws.api#service
        use aws.protocols#awsJson1_0

        @service(sdkId: "$sdkId")
        @awsJson1_0
        service $serviceName {
            operations: [],
            version: "1-alpha"
        }
    """.trimIndent()

    private val json = Json { prettyPrint = true }

    private val initialManifest = PackageManifest(
        listOf(
            PackageMetadata("Package 1", "aws.sdk.kotlin.services.package1", "package1", "AwsSdkKotlinPackage1"),
            PackageMetadata("Package 2", "aws.sdk.kotlin.services.package2", "package2", "AwsSdkKotlinPackage2"),
        ),
    )

    private fun setupTest(tempDir: File, sdkId: String, currentManifest: PackageManifest? = initialManifest): UpdatePackageManifest {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        currentManifest?.let {
            val currentManifestContents = json.encodeToString(it)
            tempDir.resolve("packages.json").writeText(currentManifestContents)
        }
        val testModelFile = tempDir.resolve("model.smithy")
        testModelFile.writeText(modelContents(sdkId))

        return project.tasks.create<UpdatePackageManifest>("updatePackageManifest") {
            modelFile.set(testModelFile)
        }
    }

    @Test
    fun testNewPackage() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val task = setupTest(tempDir, "Test Gradle")
            task.updatePackageManifest()

            val updated = PackageManifest.fromFile(tempDir.resolve("packages.json"))
            val expectedPackages = initialManifest.packages.toMutableList()
            expectedPackages.add(
                PackageMetadata("Test Gradle", "aws.sdk.kotlin.services.testgradle", "testgradle", "AwsSdkKotlinTestGradle"),
            )
            val expected = initialManifest.copy(expectedPackages)

            assertEquals(expected, updated)
        }
    }

    @Test
    fun testManifestNotExistYet() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val task = setupTest(tempDir, "Test Gradle", null)
            task.updatePackageManifest()
            val updated = PackageManifest.fromFile(tempDir.resolve("packages.json"))
            val expected = PackageManifest(
                listOf(
                    PackageMetadata("Test Gradle", "aws.sdk.kotlin.services.testgradle", "testgradle", "AwsSdkKotlinTestGradle"),
                ),
            )
            assertEquals(expected, updated)
        }
    }

    @Test
    fun testExistingPackage() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val task = setupTest(tempDir, "Package 2")
            val ex = assertFailsWith<IllegalStateException> {
                task.updatePackageManifest()
            }
            assertContains(ex.message!!, "found existing package in manifest for sdkId `Package 2`")
        }
    }

    @Test
    fun testDirectory() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val models = listOf(
                "model1.smithy" to modelContents("Package 1", "Service1"),
                "model2.smithy" to modelContents("Package 2", "Service2"),
                // non AWS service (no sdkId)
                "model3.smithy" to """
                    ${"$"}version: "2"
                    namespace gradle.test
                    service Service3 {
                        operations: [],
                        version: "1-alpha"
                    }
                """.trimIndent(),
            )

            val modelFolder = tempDir.resolve("models")
            modelFolder.mkdirs()
            models.forEach { (filename, contents) ->
                val modelFile = modelFolder.resolve(filename)
                modelFile.writeText(contents)
            }
            val task = project.tasks.create<UpdatePackageManifest>("updatePackageManifest") {
                modelDir.set(modelFolder)
            }

            task.updatePackageManifest()
            val updated = PackageManifest.fromFile(tempDir.resolve("packages.json"))
            assertEquals(initialManifest, updated)
        }
    }

    @Test
    fun testDirectoryDiscover() = runBlocking {
        withTempDir { dir ->
            val tempDir = File(dir.toString())
            val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
            val models = listOf(
                "model1.smithy" to modelContents("Package 1", "Service1"),
                "model2.smithy" to modelContents("Package 2", "Service2"),
                "model3.smithy" to modelContents("Package 3", "Service3"),
            )

            val modelFolder = tempDir.resolve("models")
            modelFolder.mkdirs()
            models.forEach { (filename, contents) ->
                val modelFile = modelFolder.resolve(filename)
                modelFile.writeText(contents)
            }

            val currentManifestContents = json.encodeToString(initialManifest)
            tempDir.resolve("packages.json").writeText(currentManifestContents)

            val task = project.tasks.create<UpdatePackageManifest>("updatePackageManifest") {
                modelDir.set(modelFolder)
                discover.set(true)
            }

            task.updatePackageManifest()
            val updated = PackageManifest.fromFile(tempDir.resolve("packages.json"))
            val expected = initialManifest.copy(
                initialManifest.packages + listOf(
                    PackageMetadata("Package 3", "aws.sdk.kotlin.services.package3", "package3", "AwsSdkKotlinPackage3"),
                ),
            )
            assertEquals(expected, updated)
        }
    }
}
