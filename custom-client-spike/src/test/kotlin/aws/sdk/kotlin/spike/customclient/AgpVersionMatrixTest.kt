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

class AgpVersionMatrixTest {
    private val repo = System.getProperty("spike.repo")
    private val pluginVersion = System.getProperty("spike.version")
    private val kotlinVersion = System.getProperty("spike.kotlinVersion")
    private val androidSdk = System.getProperty("spike.androidSdk")
    private val workDir = File(System.getProperty("spike.workDir"))
    private val testKitDir = workDir.resolve("testkit-home")

    private data class Pair(val agp: String, val gradle: String) {
        val major: Int get() = agp.substringBefore('.').toInt()
    }

    private val matrix = System.getProperty("spike.agpMatrix").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        .map { Pair(it.substringBefore('@'), it.substringAfter('@')) }

    enum class Scenario {
        /** `com.android.application` + `org.jetbrains.kotlin.android` (on AGP 9: built-in Kotlin turned off). */
        APP_KOTLIN_ANDROID,

        /** `com.android.application` only, AGP 9 built-in Kotlin. */
        APP_BUILTIN_KOTLIN,

        /** KMP with an Android target: `androidTarget()` + `com.android.library` on AGP 8, the KMP Android library plugin on AGP 9. */
        KMP_ANDROID_LIBRARY,
    }

    @TestFactory
    fun matrix(): List<DynamicTest> = matrix.flatMap { pair ->
        Scenario.entries
            .filter { !(it == Scenario.APP_BUILTIN_KOTLIN && pair.major < 9) }
            .map { scenario -> DynamicTest.dynamicTest("agp ${pair.agp} / gradle ${pair.gradle} / $scenario") { run(pair, scenario) } }
    }

    private fun run(pair: Pair, scenario: Scenario) {
        val dir = workDir.resolve("agp/${pair.agp}/${scenario.name.lowercase()}")
        dir.deleteRecursively()
        dir.mkdirs()
        writeProject(dir, pair, scenario)

        val generate = when (scenario) {
            Scenario.KMP_ANDROID_LIBRARY -> ":awsSdkKotlinGenerateCommonMain"
            Scenario.APP_KOTLIN_ANDROID -> ":awsSdkKotlinGenerateMain"
            Scenario.APP_BUILTIN_KOTLIN -> ":awsSdkKotlinGenerateDebug" // per-variant task on the Android path
        }
        val assemble = if (scenario == Scenario.KMP_ANDROID_LIBRARY) "assemble" else "assembleDebug"
        val args = arrayOf(assemble, "--configuration-cache", "--build-cache")

        try {
            val first = runner(dir, pair.gradle, *args).build()
            assertEquals(TaskOutcome.SUCCESS, first.task(generate)?.outcome, tail(first))

            val second = runner(dir, pair.gradle, *args).build()
            assertTrue(second.output.contains("Reusing configuration cache"), "CC not reused\n${tail(second)}")
            assertEquals(TaskOutcome.UP_TO_DATE, second.task(generate)?.outcome, tail(second))

            val third = runner(dir, pair.gradle, "clean", *args).build()
            assertEquals(TaskOutcome.FROM_CACHE, third.task(generate)?.outcome, tail(third))

            record(pair, scenario, "PASS", "")
        } catch (t: Throwable) {
            record(pair, scenario, "FAIL", firstErrorLine(t.message.orEmpty()))
            throw t
        }
    }

    private fun runner(dir: File, gradleVersion: String, vararg args: String) = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(dir)
        .withTestKitDir(testKitDir)
        .withArguments(*args, "--stacktrace")
        .forwardOutput()

    private fun writeProject(dir: File, pair: Pair, scenario: Scenario) {
        dir.resolve("local.properties").writeText("sdk.dir=$androidSdk\n")

        val props = buildList {
            add("android.useAndroidX=true")
            add("org.gradle.jvmargs=-Xmx2g")
            if (scenario == Scenario.APP_KOTLIN_ANDROID && pair.major >= 9) {
                add("android.builtInKotlin=false")
                add("android.newDsl=false")
            }
        }
        dir.resolve("gradle.properties").writeText(props.joinToString("\n", postfix = "\n"))

        dir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven { url = uri("${repo.replace("\\", "/")}") }
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositories { google(); mavenCentral() }
            }
            buildCache { local { directory = file("build-cache") } }
            rootProject.name = "consumer"
            """.trimIndent() + "\n",
        )

        val spec = """
            awsSdkKotlin {
                visibility.set("internal")
                service("s3", listOf("putObject", "GETOBJECT"))
            }
        """.trimIndent()

        val build = when (scenario) {
            Scenario.APP_KOTLIN_ANDROID -> """
                import aws.sdk.kotlin.spike.customclient.awsSdkKotlin

                plugins {
                    id("com.android.application") version "${pair.agp}"
                    id("org.jetbrains.kotlin.android") version "$kotlinVersion"
                    id("aws.sdk.kotlin.custom-client-spike") version "$pluginVersion"
                }

                android {
                    namespace = "com.example.consumer"
                    compileSdk = 35
                    defaultConfig { minSdk = 24 }
                }

                kotlin {
                    jvmToolchain(17)
                    sourceSets.named("main") {
                ${spec.prependIndent("        ")}
                    }
                }
            """
            Scenario.APP_BUILTIN_KOTLIN -> """
                import aws.sdk.kotlin.spike.customclient.awsSdkKotlin

                plugins {
                    id("com.android.application") version "${pair.agp}"
                    id("aws.sdk.kotlin.custom-client-spike") version "$pluginVersion"
                }

                android {
                    namespace = "com.example.consumer"
                    compileSdk = 35
                    defaultConfig { minSdk = 24 }
                    // Built-in Kotlin has no `main` KotlinSourceSet, so the spec goes on the Android source set.
                    sourceSets.named("main") {
                ${spec.prependIndent("        ")}
                    }
                }
            """
            Scenario.KMP_ANDROID_LIBRARY -> if (pair.major >= 9) {
                """
                import aws.sdk.kotlin.spike.customclient.awsSdkKotlin

                plugins {
                    id("org.jetbrains.kotlin.multiplatform") version "$kotlinVersion"
                    id("com.android.kotlin.multiplatform.library") version "${pair.agp}"
                    id("aws.sdk.kotlin.custom-client-spike") version "$pluginVersion"
                }

                kotlin {
                    android {
                        namespace = "com.example.consumer"
                        compileSdk = 35
                        minSdk = 24
                    }
                    jvm()
                    sourceSets.named("commonMain") {
                ${spec.prependIndent("        ")}
                    }
                }
                """
            } else {
                """
                import aws.sdk.kotlin.spike.customclient.awsSdkKotlin

                plugins {
                    id("org.jetbrains.kotlin.multiplatform") version "$kotlinVersion"
                    id("com.android.library") version "${pair.agp}"
                    id("aws.sdk.kotlin.custom-client-spike") version "$pluginVersion"
                }

                android {
                    namespace = "com.example.consumer"
                    compileSdk = 35
                    defaultConfig { minSdk = 24 }
                }

                kotlin {
                    jvmToolchain(17)
                    androidTarget()
                    jvm()
                    sourceSets.named("commonMain") {
                ${spec.prependIndent("        ")}
                    }
                }
                """
            }
        }.trimIndent()
        dir.resolve("build.gradle.kts").writeText(build + "\n")

        if (scenario != Scenario.KMP_ANDROID_LIBRARY || pair.major < 9) {
            val manifestDir = if (scenario == Scenario.KMP_ANDROID_LIBRARY) "src/androidMain" else "src/main"
            dir.resolve(manifestDir).apply { mkdirs() }.resolve("AndroidManifest.xml").writeText("<manifest/>\n")
        }
        val srcRoot = if (scenario == Scenario.KMP_ANDROID_LIBRARY) "src/commonMain/kotlin" else "src/main/kotlin"
        dir.resolve(srcRoot).apply { mkdirs() }.resolve("App.kt").writeText(
            """
            import aws.sdk.kotlin.services.s3.S3Client
            import kotlinx.coroutines.Dispatchers

            internal fun use(): String = S3Client().putObject() + S3Client().getObject() + Dispatchers.Default
            """.trimIndent() + "\n",
        )
    }

    private fun tail(r: BuildResult) = r.output.lines().takeLast(40).joinToString("\n")

    private fun firstErrorLine(msg: String): String {
        val lines = msg.lines()
        val idx = lines.indexOfFirst { it.contains("What went wrong") }
        if (idx >= 0) return lines.drop(idx + 1).take(4).joinToString(" | ") { it.trim() }.take(400)
        return (lines.firstOrNull { it.startsWith("e: ") || it.contains("expected") || it.contains("Exception") } ?: lines.firstOrNull().orEmpty()).trim().take(300)
    }

    private fun record(pair: Pair, scenario: Scenario, verdict: String, detail: String) = synchronized(AgpVersionMatrixTest::class.java) {
        workDir.mkdirs()
        workDir.resolve("agp-results.tsv").appendText("${pair.agp}\t${pair.gradle}\t$scenario\t$verdict\t${detail.replace('\t', ' ')}\n")
    }
}
