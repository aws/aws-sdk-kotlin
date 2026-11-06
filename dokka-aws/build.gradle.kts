/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    `dokka-convention`
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

description = "Custom Dokka plugin for AWS Kotlin SDK API docs"

dependencies {
    compileOnly(libs.dokka.base)
    compileOnly(libs.dokka.core)

    testImplementation(libs.jsoup)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    listOf("html", "gfm").forEach { format ->
        val publicationTask = "dokkaGeneratePublication${format.replaceFirstChar(Char::titlecase)}"
        val outputDir = tasks.named<DokkaGeneratePublicationTask>(publicationTask).flatMap { it.outputDirectory }

        inputs.dir(outputDir).withPropertyName("dokka-$format").withPathSensitivity(PathSensitivity.RELATIVE)
        jvmArgumentProviders.add(
            CommandLineArgumentProvider { listOf("-Ddokka.$format.outputDir=${outputDir.get().asFile.absolutePath}") },
        )
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=1.8")
        allWarningsAsErrors.set(false) // FIXME Dokka bundles stdlib into the classpath, causing an unfixable warning
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}
