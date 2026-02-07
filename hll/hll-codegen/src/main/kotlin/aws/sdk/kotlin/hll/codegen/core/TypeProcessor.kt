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

    private fun formatTypeName(type: Type): String {
        if (type !is TypeRef) return type.shortName

        val matchingImport = imports.getByFullName(type.fullBaseName)
        if (matchingImport != null) return buildString { // Already imported
            append(matchingImport.shortName)
            if (type.shortName.contains('.')) {
                append('.')
                append(type.shortName.substringAfter('.'))
            }
        }

        val conflictingImport = imports.getByShortName(type.shortBaseName)
        if (conflictingImport != null) return type.fullName

        if (type.pkg != pkg && type.pkg != "kotlin") imports += ImportDirective(type)
        return type.shortName
    }

    private fun formatType(type: Type, forDeclaration: Boolean): String = buildString {
        append(formatTypeName(type))
        when (type) {
            is TypeRef -> append(formatGenerics(type.genericArgs, forDeclaration))
            is TypeVar if type.constraintType != null && forDeclaration -> {
                append(" : ")
                append(formatType(type.constraintType, true))
            }
            else -> {}
        }
        if (type.nullable) append('?')
    }
}
