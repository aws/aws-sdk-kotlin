/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.ksp.processors.HllKspProcessor
import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions.VisibilityAttribute
import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.DestinationPackageAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateBuilderClassesAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateGetTableMethodAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering.HighLevelRenderer
import aws.smithy.kotlin.runtime.collections.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

private val annotationName = DynamoDbItem::class.qualifiedName!!

public class AnnotationsProcessor(private val environment: SymbolProcessorEnvironment) : HllKspProcessor(environment) {
    private val logger = environment.logger

    override fun processImpl(resolver: Resolver): List<KSAnnotated> {
        logger.info("Searching for symbols annotated with $annotationName")
        val annotated = resolver.getSymbolsWithAnnotation(annotationName)
        val invalid = annotated.filterNot { it.validate() }.toList()
        logger.info("Found invalid classes $invalid")

        val annotatedClasses = annotated
            .toList()
            .also { logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }

        val dependencies = Dependencies(aggregating = true, *(annotatedClasses.mapNotNull { it.containingFile }.toTypedArray()))
        val codeGeneratorFactory = CodeGeneratorFactory(environment.codeGenerator, logger, dependencies)

        HighLevelRenderer(annotatedClasses, logger, codeGeneratorFactory, getCodegenAttributes()).render()

        return invalid
    }

    /**
     * Parse and validate the KSP environment options, turning them into valid attribute values
     */
    private fun getCodegenAttributes(): Attributes {
        val generateGetTableMethod = environment.options.getOrDefault(GenerateGetTableMethodAttribute.name, "true")
        check(generateGetTableMethod.equals("true", ignoreCase = true) || generateGetTableMethod.equals("false", ignoreCase = true)) { "Unsupported value for ${GenerateGetTableMethodAttribute.name}, expected \"true\" or \"false\", got $generateGetTableMethod" }

        return attributesOf {
            GenerateBuilderClassesAttribute to GenerateBuilderClasses.valueOf(environment.options[GenerateBuilderClassesAttribute.name] ?: GenerateBuilderClasses.WHEN_REQUIRED.name)
            VisibilityAttribute to Visibility.valueOf(environment.options.getOrDefault(VisibilityAttribute.name, Visibility.PUBLIC.name))
            DestinationPackageAttribute to DestinationPackage.fromString(environment.options.getOrDefault(DestinationPackageAttribute.name, "relative=aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas"))
            GenerateGetTableMethodAttribute to generateGetTableMethod.equals("true", ignoreCase = true)
        }
    }
}
