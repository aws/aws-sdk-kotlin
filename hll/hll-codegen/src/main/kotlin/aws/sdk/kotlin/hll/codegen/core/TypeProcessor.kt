/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.model.GenericsSet
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar

/**
 * Processes types into formatted Kotlin code via three internal [TemplateProcessor] instances. This processor will
 * import types where possible to shorten references in generated code.
 * @param pkg The Kotlin package namespace for the file which will be generated. This is used when deciding whether/how
 * to import type definitions.
 * @param imports A collection of import directives for the file which will be generated. This collection will be
 * appended by this processor as types are imported.
 */
internal class TypeProcessor(private val pkg: String, private val imports: ImportDirectives) {
    /**
     * A [TemplateProcessor] which formats a set of generics into code that can be used in a declaration. The output
     * will be in the format `<A, B : C, D> `. Note that this processor will output a trailing blank space unless the
     * generics set is empty.
     */
    val genericsListProcessor = TemplateProcessor.typed<GenericsSet>('G') {
        val generics = formatGenerics(it, true)
        if (generics.isEmpty()) "" else "$generics "
    }

    /**
     * A [TemplateProcessor] which formats a [Type] into Kotlin code suitable for usage as a _declaration_ (as opposed
     * to a type _reference_). This is nearly identical to [typeUsageProcessor] except:
     * * If the type is a [TypeRef], any containing type names will be omitted. For instance, a [TypeRef] whose short
     *   name is `Foo.Bar.Baz` will be emitted as `Baz`.
     * * [TypeVar] generics lists will include any relevant type constraints
     */
    val typeDeclarationProcessor = TemplateProcessor.typed<Type>('D') { formatType(it, true) }

    /**
     * A [TemplateProcessor] which formats a [Type] into Kotlin code suitable for usage as a _reference_ (as opposed to
     * a type _declaration_)
     */
    val typeUsageProcessor = TemplateProcessor.typed<Type>('T') { formatType(it, false) }

    private fun formatGenerics(generics: Collection<Type>, forDeclaration: Boolean) = when {
        generics.isEmpty() -> ""
        else -> generics.joinToString(", ", "<", ">") { formatType(it, forDeclaration) }
    }

    private fun formatTypeName(type: Type, forDeclaration: Boolean): String {
        if (type !is TypeRef) return type.shortName // TypeVars don't need to be imported
        if (forDeclaration) return type.leafName // Declarations don't need to be imported

        val matchingImport = imports.getByFullName(type.fullBaseName)
        if (matchingImport != null) { // Already imported
            return buildString {
                append(matchingImport.shortName)
                if (type.shortName.contains('.')) {
                    append('.')
                    append(type.shortName.substringAfter('.'))
                }
            }
        }

        val conflictingImport = imports.getByShortName(type.shortBaseName)
        if (conflictingImport != null) return type.fullName // Cannot import because of conflict; use full name

        if (type.pkg != pkg && type.pkg != "kotlin") imports += ImportDirective(type.fullBaseName) // Import it!
        return type.shortName
    }

    private fun formatType(type: Type, forDeclaration: Boolean): String = buildString {
        append(formatTypeName(type, forDeclaration))

        if (type is TypeRef) {
            append(formatGenerics(type.genericArgs, forDeclaration))
        } else if (type is TypeVar && type.constraintType != null && forDeclaration) {
            append(" : ")
            append(formatType(type.constraintType, false))
        }

        if (type.nullable) append('?')
    }
}
