/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.util.quote
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Defines a template processor which maps an argument value of any type to a string value
 * @param key The identifier for this processor, which will be used by the [TemplateEngine] to match a parameter with
 * this processor
 * @param handler A function that accepts an input argument (as an [Any]) and returns a formatted string
 */
@InternalSdkApi
public data class TemplateProcessor(val key: Char, val handler: (Any) -> String) {
    @InternalSdkApi
    public companion object {
        /**
         * Instantiate a new typed template processor which only receives arguments of a specific type [T]
         * @param T The type of argument values this processor will accept
         * @param key The identifier for this processor, which will be used by the [TemplateEngine] to match a parameter
         * with this processor
         * @param handler A function that accepts an input argument of type [T] and returns a formatted string
         */
        public inline fun <reified T> typed(key: Char, crossinline handler: (T) -> String): TemplateProcessor =
            TemplateProcessor(key) { value ->
                require(value is T) { "Expected argument of type ${T::class} but found $value" }
                handler(value)
            }

        /**
         * A literal template processor. This processor substitutes parameters in the form of `#L` with the [toString]
         * representation of the corresponding argument.
         */
        public val Literal: TemplateProcessor = TemplateProcessor('L') { it.toString() }

        /**
         * A quoted string template processor. This processor substitutes parameters in the form of `#S` with the
         * quoted/escaped form of a string argument. See [quote] for more details.
         */
        public val QuotedString: TemplateProcessor = typed<String>('S') { it.quote() }
    }

    init {
        require(key in 'A'..'Z') { "Key character must be a capital letter (A-Z)" }
    }
}

/*
private open class TypeProcessor(private val includeConstraints: Boolean) {
    open fun getTypeName(type: Type): String = type.shortName

    fun format(type: Type): String = buildString {
        append(getTypeName(type))

        if (type is TypeRef && type.genericArgs.isNotEmpty()) {
            type.genericArgs.joinToString(", ", "<", ">", transform = ::format).let(::append)
        }

        if (type.nullable) append('?')
    }
}

private class ImportingTypeProcessor(private val pkg: String, private val imports: ImportDirectives) : TypeProcessor(false) {
    override fun getTypeName(type: Type): String {
        if (type !is TypeRef) return super.getTypeName(type)

        val matchingImport = imports.getByFullName(type.fullBaseName)
        if (matchingImport != null) return buildString {
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
}

private class GenericsListProcessor(private val importProcessor: TemplateProcessor) {
    fun format(types: Collection<TypeVar>): String = buildString {
        if (types.isNotEmpty()) {
            append('<')

            types.forEachIndexed { idx, type ->
                if (idx > 0)  append(", ")

                append(type.shortName)
                if (type.constraintType != null) {
                    append(" : ")
                    append(importProcessor.handler(type.constraintType))
                }
            }

            append("> ")
        }
    }
}
*/