/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.runtime.InternalSdkApi
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.CodeGenerator as KSCodeGenerator

/**
 * A factory for [CodeGenerator] instances which will be backed by a [KSCodeGenerator] instance
 * @param ksCodeGenerator The underlying KSP [KSCodeGenerator] to use for low-level file access and dependency tracking
 * @param logger A logger instance to use for message
 */
@InternalSdkApi
public class CodeGeneratorFactory(
    private val ksCodeGenerator: KSCodeGenerator,
    private val logger: KSPLogger,
    private val dependencies: Dependencies = Dependencies.ALL_FILES,
) {
    /**
     * Creates a new [CodeGenerator] backed by a [KSCodeGenerator]. The returned generator starts with no imports and
     * uses a configured [TemplateEngine] with the default set of processors.
     * @param fileName The name of the file which should be created _without_ parent directory or extension (which is always
     * **.kt**)
     * @param pkg The Kotlin package for the generated code (e.g., `aws.sdk.kotlin.hll.dynamodbmapper.operations`)
     * @param codeGeneratorName The name of this [CodeGenerator]
     */
    public fun generator(fileName: String, pkg: String, codeGeneratorName: String): CodeGenerator {
        val imports = ImportDirectives()
        val typeProcessor = TypeProcessor(pkg, imports)
        val processors = listOf(
            TemplateProcessor.Literal,
            TemplateProcessor.QuotedString,
            typeProcessor.typeUsageProcessor,
            typeProcessor.genericsListProcessor,
            typeProcessor.typeDeclarationProcessor,
        )
        val engine = TemplateEngine(processors)

        val persistCallback: (String) -> Unit = { content ->
            logger.info("Checking out code generator for class $pkg.$fileName")

            ksCodeGenerator
                .createNewFile(dependencies, pkg, fileName)
                .use { outputStream ->
                    outputStream.writer().use { writer -> writer.append(content) }
                }
        }

        return CodeGeneratorImpl(pkg, engine, persistCallback, imports, codeGeneratorName)
    }
}
