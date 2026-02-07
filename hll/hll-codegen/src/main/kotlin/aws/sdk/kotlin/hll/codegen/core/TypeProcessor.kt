/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.model.GenericsSet
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar

internal class TypeProcessor(private val pkg: String, private val imports: ImportDirectives) {
    val genericsListProcessor = TemplateProcessor.typed<GenericsSet>('G') {
        val generics = formatGenerics(it, true)
        if (generics.isEmpty()) "" else "$generics "
    }

    val typeUsageProcessor = TemplateProcessor.typed<Type>('T') { formatType(it, false) }
    val typeDeclarationProcessor = TemplateProcessor.typed<Type>('D') { formatType(it, true) }

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

        if (type.pkg != pkg && type.pkg != "kotlin") imports += ImportDirective(type) // No conflicts; import it
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
