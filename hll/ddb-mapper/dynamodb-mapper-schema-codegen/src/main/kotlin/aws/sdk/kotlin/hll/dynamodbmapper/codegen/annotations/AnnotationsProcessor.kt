/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.ksp.KspCodegenLogger
import aws.sdk.kotlin.hll.codegen.ksp.processors.HllKspProcessor
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions.VisibilityAttribute
import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.DestinationPackageAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateBuilderClassesAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateGetTableMethodAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction.DiagnosticCollector
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction.buildModel
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.model.MappedItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering.HighLevelRenderer
import aws.smithy.kotlin.runtime.collections.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

private val annotationName = DynamoDbItem::class.qualifiedName!!
private val mappableAnnotationName = DynamoDbMappable::class.qualifiedName!!

/**
 * Builds the IR for [classes] and reports every diagnostic to [ctx]'s logger. Diagnostics are flushed even if
 * extraction throws, so problems collected before an internal failure are never lost.
 * @return the IR to render, or `null` if any error was reported
 */
internal fun extractModel(classes: List<KSClassDeclaration>, ctx: RenderContext): List<MappedItem>? {
    val diagnostics = DiagnosticCollector()
    val items = try {
        buildModel(classes, ctx, diagnostics)
    } finally {
        diagnostics.flushTo(ctx.logger)
    }
    return items.takeUnless { diagnostics.hasErrors }
}

public class AnnotationsProcessor(private val environment: SymbolProcessorEnvironment) : HllKspProcessor(environment) {
    private val logger = environment.logger

    override fun processImpl(resolver: Resolver): List<KSAnnotated> {
        logger.info("Searching for symbols annotated with $annotationName or $mappableAnnotationName")

        // Discover both table items (@DynamoDbItem) and nested-only mappable types (@DynamoDbMappable). KSP's
        // getSymbolsWithAnnotation only matches directly-applied annotations, so the meta-annotation on @DynamoDbItem
        // is not expanded here; we query for both explicitly. A class annotated with @DynamoDbItem is not *directly*
        // annotated with @DynamoDbMappable, so the two result sets are disjoint except when a user redundantly applies
        // both -- distinctBy guards against rendering such a class twice.
        val annotated = (
            resolver.getSymbolsWithAnnotation(annotationName) +
                resolver.getSymbolsWithAnnotation(mappableAnnotationName)
            ).toList()

        val invalid = annotated.filterNot { it.validate() }
        logger.info("Found invalid classes $invalid")

        val annotatedClasses = annotated
            .also { logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .distinctBy { it.qualifiedName?.asString() }

        val dependencies = Dependencies(aggregating = true, *(annotatedClasses.mapNotNull { it.containingFile }.toTypedArray()))
        val codeGeneratorFactory = CodeGeneratorFactory(environment.codeGenerator, logger, dependencies)

        // The base context carries processor-wide state; each item's destination package is applied at render time.
        val ctx = RenderContext(
            KspCodegenLogger(logger),
            codeGeneratorFactory,
            pkg = "",
            rendererName = "dynamodb-mapper-annotation-processor",
            attributes = getCodegenAttributes(),
        )

        // Extract and validate everything first so all problems are reported together. Nothing is rendered if any
        // error was found.
        extractModel(annotatedClasses, ctx)?.let { items -> HighLevelRenderer(ctx, items).render() }

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
