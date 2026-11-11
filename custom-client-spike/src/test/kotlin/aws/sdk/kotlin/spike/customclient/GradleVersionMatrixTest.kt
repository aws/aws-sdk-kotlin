/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.spike.customclient

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GradleVersionMatrixTest {
    private val repo = System.getProperty("spike.repo")
    private val pluginVersion = System.getProperty("spike.version")
    private val kotlinVersion = System.getProperty("spike.kotlinVersion")
    private val gradleVersions = System.getProperty("spike.gradleVersions").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    private val workDir = File(System.getProperty("spike.workDir"))

    // One persistent TestKit home so KGP and friends download once, not per test.
    private val testKitDir = workDir.resolve("testkit-home")

    enum class Scenario { KMP_SET, JVM_SET, KMP_ASSIGN, KMP_BAD_OPERATION }

    @TestFactory
    fun matrix(): List<DynamicTest> = gradleVersions.flatMap { gv ->
        Scenario.entries.map { scenario ->
            DynamicTest.dynamicTest("gradle $gv / $scenario") { runScenario(gv, scenario) }
        }
    }

    private fun runScenario(gradleVersion: String, scenario: Scenario) {
        val dir = workDir.resolve("projects/$gradleVersion/${scenario.name.lowercase()}")
        dir.deleteRecursively()
        dir.mkdirs()
        writeProject(dir, scenario)

        try {
            when (scenario) {
                Scenario.KMP_BAD_OPERATION -> {
                    val result = runner(dir, gradleVersion, "help").buildAndFail()
                    assertTrue(
                        result.output.contains("Unknown operation 'putObjekt' for service S3"),
                        "expected unknown-operation error\n${tail(result)}",
                    )
                }
                else -> {
                    val compile = if (scenario == Scenario.JVM_SET) ":compileKotlin" else ":compileKotlinJvm"
                    val generate = if (scenario == Scenario.JVM_SET) ":awsSdkKotlinGenerateMain" else ":awsSdkKotlinGenerateCommonMain"

                    val first = runner(dir, gradleVersion, "build", "--configuration-cache", "--build-cache").build()
                    assertEquals(TaskOutcome.SUCCESS, first.task(generate)?.outcome, tail(first))
                    assertEquals(TaskOutcome.SUCCESS, first.task(compile)?.outcome, tail(first))
                    assertTrue(dir.resolve("build/reports/aws-sdk-kotlin/lock.json").readText().contains("\"service\": \"S3\""))

                    val second = runner(dir, gradleVersion, "build", "--configuration-cache", "--build-cache").build()
                    assertTrue(second.output.contains("Reusing configuration cache"), "CC not reused\n${tail(second)}")
                    assertEquals(TaskOutcome.UP_TO_DATE, second.task(generate)?.outcome, tail(second))

                    val third = runner(dir, gradleVersion, "clean", "build", "--configuration-cache", "--build-cache").build()
                    assertEquals(TaskOutcome.FROM_CACHE, third.task(generate)?.outcome, tail(third))
                }
            }
            record(gradleVersion, scenario, "PASS", "")
        } catch (t: Throwable) {
            record(gradleVersion, scenario, "FAIL", firstErrorLine(t.message.orEmpty()))
            throw t
        }
    }

    private fun runner(dir: File, gradleVersion: String, vararg args: String) = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(dir)
        .withTestKitDir(testKitDir)
        .withArguments(*args, "--stacktrace")
        .forwardOutput()

    private fun writeProject(dir: File, scenario: Scenario) {
        val kmp = scenario != Scenario.JVM_SET
        dir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("${repo.replace("\\", "/")}") }
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            buildCache { local { directory = file("build-cache") } }
            rootProject.name = "consumer"
            """.trimIndent(),
        )

        val spec = when (scenario) {
            Scenario.KMP_ASSIGN -> """
                visibility = "internal"
                generatePaginators = false
                service("s3", listOf("putObject", "GETOBJECT"))
            """
            Scenario.KMP_BAD_OPERATION -> """
                service("s3", listOf("putObjekt"))
            """
            else -> """
                visibility.set("internal")
                generatePaginators.set(false)
                service("s3", listOf("putObject", "GETOBJECT"))
            """
        }.trimIndent().prependIndent("                ")

        val kotlinBlock = if (kmp) {
            """
            kotlin {
                jvm()
                sourceSets.named("commonMain") {
                    awsSdkKotlin {
            $spec
                    }
                }
            }
            """
        } else {
            """
            kotlin {
                sourceSets.named("main") {
                    awsSdkKotlin {
            $spec
                    }
                }
            }
            """
        }.trimIndent()

        dir.resolve("build.gradle.kts").writeText(
            """
            import aws.sdk.kotlin.spike.customclient.awsSdkKotlin

            plugins {
                kotlin("${if (kmp) "multiplatform" else "jvm"}") version "$kotlinVersion"
                id("aws.sdk.kotlin.custom-client-spike") version "$pluginVersion"
            }

            repositories { mavenCentral() }

            """.trimIndent() + "\n" + kotlinBlock + "\n",
        )

        val src = dir.resolve(if (kmp) "src/commonMain/kotlin" else "src/main/kotlin").apply { mkdirs() }
        src.resolve("App.kt").writeText(
            """
            import aws.sdk.kotlin.services.s3.S3Client
            import kotlinx.coroutines.Dispatchers

            internal fun use(): String = S3Client().putObject() + S3Client().getObject() + Dispatchers.Default
            """.trimIndent() + "\n",
        )
    }

    private fun tail(r: BuildResult) = r.output.lines().takeLast(40).joinToString("\n")

    private fun firstErrorLine(msg: String): String = msg.lines()
        .firstOrNull { it.contains("error", true) || it.contains("Exception") || it.startsWith("e:") || it.contains("What went wrong") || it.contains("expected") }
        ?.trim()?.take(220) ?: msg.lines().firstOrNull()?.take(220).orEmpty()

    private fun record(gv: String, scenario: Scenario, verdict: String, detail: String) = synchronized(Companion) {
        workDir.mkdirs()
        workDir.resolve("results.tsv").appendText("$gv\t$scenario\t$verdict\t${detail.replace('\t', ' ')}\n")
    }

    private companion object
}
