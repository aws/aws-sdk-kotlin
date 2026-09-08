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
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

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
        // Cyclic nesting (including self-references) is not yet supported: the generated converters would form a
        // construction-time initialization cycle. Detect and reject such types with a compile-time error.
        val cyclicClasses = findCyclicallyNestedClasses()

        annotatedClasses.forEach { annotated ->
            logger.info("Processing annotation on ${annotated.simpleName}")

            val qualifiedName = annotated.qualifiedName?.asString()
            if (qualifiedName != null && qualifiedName in cyclicClasses) {
                logger.error(
                    "Cyclic nesting detected involving type '$qualifiedName'. Nesting " +
                        "@DynamoDbItem/@DynamoDbMappabletypes in a reference cycle (including self-references) is " +
                        "not yet supported.",
                    annotated,
                )
                return@forEach
            }

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

    /**
     * Returns the qualified names of annotated classes that participate in a nesting reference cycle (a type that can
     * transitively reference itself through its mappable properties, including direct self-references).
     */
    private fun findCyclicallyNestedClasses(): Set<String> {
        val classesByName = annotatedClasses.associateBy { requireNotNull(it.qualifiedName).asString() }

        // Build adjacency limited to edges between annotated (mappable) types.
        val adjacency = classesByName.mapValues { (_, decl) ->
            decl.getAllProperties()
                .flatMap { it.type.resolve().referencedTypeNames() }
                .filter { it in classesByName }
                .toSet()
        }

        return classesByName.keys.filterTo(mutableSetOf()) { start ->
            // A class is cyclic if it can reach itself by following references.
            val visited = mutableSetOf<String>()
            val toVisit = ArrayDeque(adjacency[start].orEmpty())
            var reachesSelf = false
            while (toVisit.isNotEmpty()) {
                val current = toVisit.removeLast()
                if (current == start) {
                    reachesSelf = true
                    break
                }
                if (visited.add(current)) {
                    toVisit.addAll(adjacency[current].orEmpty())
                }
            }
            reachesSelf
        }
    }

    // Value converters must be generated for any mappable type which is referenced by another annotated type
    private val KSClassDeclaration.shouldRenderValueConverter: Boolean
        get() {
            val name = requireNotNull(qualifiedName).asString()

            return annotatedClasses.any { otherClass ->
                otherClass.getAllProperties().any { prop ->
                    // Recurse through the full type-argument tree so references nested inside collections
                    // (e.g. List<Map<String, Nested>>) are detected at any depth.
                    prop.type.resolve().referencesType(name)
                }
            }
        }

    private val KSType.fqName: String?
        get() = declaration.qualifiedName?.asString()

    private fun KSTypeReference.referencesType(qualifiedName: String): Boolean = resolve().referencesType(qualifiedName)

    private fun KSType.referencesType(qualifiedName: String): Boolean = fqName == qualifiedName || arguments.any {
        it.type?.referencesType(qualifiedName) == true
    }

    /** Yields the qualified names of this type and all of its type arguments, recursively. */
    private fun KSType.referencedTypeNames(): Sequence<String> = sequence {
        declaration.qualifiedName?.asString()?.let { yield(it) }
        arguments.forEach { arg -> arg.type?.resolve()?.let { yieldAll(it.referencedTypeNames()) } }
    }
}
