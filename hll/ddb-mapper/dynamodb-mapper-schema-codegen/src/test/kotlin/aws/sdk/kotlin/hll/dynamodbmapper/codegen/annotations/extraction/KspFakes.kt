/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.core.CodegenLogger
import aws.sdk.kotlin.hll.codegen.core.SymbolRef
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
import aws.smithy.kotlin.runtime.collections.attributesOf
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mockk.every
import io.mockk.mockk

/*
 * Minimal fakes for the KSP surface read by the extraction stage and by hll-codegen's `Type.from(KSType)`. Each helper
 * stubs exactly what those code paths read (see the Phase 2 checklist in CODEGEN_REARCH_IMPL_PLAN.md); anything else
 * is left to a strict mock so an unexpected read fails loudly instead of silently returning a default.
 */

internal fun fakeName(fqName: String): KSName = mockk {
    every { asString() } returns fqName
    every { getQualifier() } returns fqName.substringBeforeLast('.', "")
    every { getShortName() } returns fqName.substringAfterLast('.')
}

/**
 * A fake class declaration
 * @param annotations Annotations present on the declaration
 * @param hasSourceFile Whether the declaration originates from source in the current module
 */
internal fun fakeClassDeclaration(
    fqName: String,
    classKind: ClassKind = ClassKind.CLASS,
    annotations: List<KSAnnotation> = emptyList(),
    hasSourceFile: Boolean = true,
    properties: List<KSPropertyDeclaration> = emptyList(),
    hasZeroArgConstructor: Boolean = true,
): KSClassDeclaration = mockk {
    val constructor = mockk<KSFunctionDeclaration> {
        every { simpleName } returns fakeName("<init>")
        every { parameters } returns if (hasZeroArgConstructor) {
            emptyList()
        } else {
            listOf(mockk<KSValueParameter> { every { hasDefault } returns false })
        }
    }
    every { declarations } answers { sequenceOf(constructor) }
    every { getAllProperties() } answers { properties.asSequence() }
    every { asStarProjectedType() } answers { fakeKsType(fqName, declaration = self as KSClassDeclaration) }
    every { qualifiedName } returns fakeName(fqName)
    every { simpleName } returns fakeName(fqName.substringAfterLast('.'))
    every { packageName } returns fakeName(fqName.substringBeforeLast('.', ""))
    every { parentDeclaration } returns null
    every { this@mockk.classKind } returns classKind
    every { this@mockk.annotations } answers { annotations.asSequence() }
    every { containingFile } returns if (hasSourceFile) mockk<KSFile>() else null
    every { this@mockk.toString() } returns fqName.substringAfterLast('.')
}

internal fun fakeTypeReference(type: KSType): KSTypeReference = mockk {
    every { resolve() } returns type
}

/**
 * A fake type. Its declaration defaults to a plain class named [fqName]; pass [declaration] to share one (e.g., to
 * attach annotations or mark it as an enum).
 */
internal fun fakeKsType(
    fqName: String,
    nullable: Boolean = false,
    args: List<KSType> = emptyList(),
    declaration: KSClassDeclaration = fakeClassDeclaration(fqName),
): KSType {
    val typeArgs = args.map { arg -> mockk<KSTypeArgument> { every { type } returns fakeTypeReference(arg) } }
    lateinit var nonNull: KSType
    val self = mockk<KSType> {
        every { this@mockk.declaration } returns declaration
        every { arguments } returns typeArgs
        every { isMarkedNullable } returns nullable
        every { makeNotNullable() } answers { nonNull }
        every { this@mockk.toString() } returns declaration.toString()
    }
    nonNull = if (nullable) fakeKsType(fqName, nullable = false, args = args, declaration = declaration) else self
    return self
}

/**
 * A fake annotation application.
 * @param annotationDeclaration The annotation class; its own annotations are its meta-annotations
 * @param arguments Named arguments (KSP models an annotation's properties as its arguments)
 */
internal fun fakeAnnotation(
    annotationDeclaration: KSClassDeclaration,
    arguments: Map<String, Any?> = emptyMap(),
): KSAnnotation {
    val annotationType = mockk<KSType> {
        every { declaration } returns annotationDeclaration
        every { this@mockk.arguments } returns emptyList()
        every { isMarkedNullable } returns false
    }
    val ksArguments = arguments.map { (argName, argValue) ->
        mockk<KSValueArgument> {
            every { name } returns fakeName(argName)
            every { value } returns argValue
        }
    }
    return mockk {
        every { shortName } returns fakeName(annotationDeclaration.qualifiedName!!.getShortName())
        every { this@mockk.annotationType } returns fakeTypeReference(annotationType)
        every { this@mockk.arguments } returns ksArguments
    }
}

internal fun fakeAnnotationClass(fqName: String, metaAnnotations: List<KSAnnotation> = emptyList()) = fakeClassDeclaration(fqName, ClassKind.ANNOTATION_CLASS, metaAnnotations)

internal fun fakeProperty(
    name: String,
    type: KSType,
    annotations: List<KSAnnotation> = emptyList(),
    mutable: Boolean = true,
): KSPropertyDeclaration = mockk {
    every { modifiers } returns emptySet()
    every { isMutable } returns mutable
    every { simpleName } returns fakeName(name)
    every { this@mockk.type } returns fakeTypeReference(type)
    every { this@mockk.annotations } answers { annotations.asSequence() }
}

/** A [CodegenLogger] which records every message, in order, for assertions */
internal class RecordingLogger : CodegenLogger {
    data class Entry(val level: String, val message: String, val symbol: SymbolRef?)

    val entries = mutableListOf<Entry>()
    val errors: List<Entry>
        get() = entries.filter { it.level == "error" }

    override fun logging(message: String, symbol: SymbolRef?) {
        entries += Entry("logging", message, symbol)
    }
    override fun info(message: String, symbol: SymbolRef?) {
        entries += Entry("info", message, symbol)
    }
    override fun warn(message: String, symbol: SymbolRef?) {
        entries += Entry("warn", message, symbol)
    }
    override fun error(message: String, symbol: SymbolRef?) {
        entries += Entry("error", message, symbol)
    }
    override fun exception(e: Throwable) = throw e
}

internal fun testRenderContext(
    logger: CodegenLogger = RecordingLogger(),
    destinationPackage: DestinationPackage = DestinationPackage.Relative("dynamodbmapper.generatedschemas"),
): RenderContext = RenderContext(
    logger = logger,
    codegenFactory = mockk<CodeGeneratorFactory>(),
    pkg = "org.example.dynamodbmapper.generatedschemas",
    attributes = attributesOf {
        AnnotationsProcessorOptions.DestinationPackageAttribute to destinationPackage
        AnnotationsProcessorOptions.GenerateBuilderClassesAttribute to GenerateBuilderClasses.WHEN_REQUIRED
        AnnotationsProcessorOptions.GenerateGetTableMethodAttribute to true
    },
)
