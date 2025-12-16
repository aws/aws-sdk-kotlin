/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.smithy.kotlin.runtime.collections.*
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param annotatedClasses A list of annotated classes
 */
internal class HighLevelRenderer(
    private val annotatedClasses: List<KSClassDeclaration>,
    private val logger: KSPLogger,
    private val codegenFactory: CodeGeneratorFactory,
    private val codegenAttributes: Attributes = emptyAttributes(),
) {
    internal fun render() {
        annotatedClasses.forEach { annotated ->
            logger.info("Processing annotation on ${annotated.simpleName}")

            val codegenPkg = when (val dstPkg = codegenAttributes[AnnotationsProcessorOptions.DestinationPackageAttribute]) {
                is DestinationPackage.Relative -> "${annotated.packageName.asString()}.${dstPkg.pkg}"
                is DestinationPackage.Absolute -> dstPkg.pkg
            }

            val attributes = codegenAttributes + (SchemaAttributes.ShouldRenderValueConverterAttribute to annotated.shouldRenderValueConverter)

            val renderCtx = RenderContext(
                logger,
                codegenFactory,
                codegenPkg,
                "dynamodb-mapper-annotation-processor",
                attributes,
            )

            val annotation = SchemaRenderer(annotated, renderCtx)
            annotation.render()
        }
    }

    // Value converters must be generated for any DynamoDbItem which is referenced by another DynamoDbItem
    private val KSClassDeclaration.shouldRenderValueConverter: Boolean
        get() = annotatedClasses.any { otherClass ->
            val name = requireNotNull(qualifiedName).asString()

            otherClass.getAllProperties().any { prop ->
                val propType = prop.type.resolve()
                val propName = requireNotNull(propType.declaration.qualifiedName).asString()

                // If the property OR any of its arguments reference the annotated type
                propName == name ||
                    propType.arguments.any { arg ->
                        val argType = arg.type?.resolve()
                        val argName = requireNotNull(argType?.declaration?.qualifiedName).asString()
                        argName == name
                    }
            }
        }
}
