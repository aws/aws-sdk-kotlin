/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
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
        public inline fun <reified T> typed(key: Char, crossinline handler: (T) -> String): TemplateProcessor = TemplateProcessor(key) { value ->
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

        /**
         * Creates a template processor for [Type] values. This processor substitutes parameters in the form of `#T`
         * with the name or an alias of the type. It also appends `import` directives if necessary.
         * @param pkg The Kotlin package into which code is being generated. An `import` directive will not be added if
         * a passed argument has the same package as this processor.
         * @param imports An [ImportDirectives] collection to which new imports will be appended
         */
        public fun forType(pkg: String, imports: ImportDirectives): TemplateProcessor {
            val processor = ImportingTypeProcessor(pkg, imports)
            return typed<Type>('T', processor::format)
        }
    }

    init {
        require(key in 'A'..'Z') { "Key character must be a capital letter (A-Z)" }
    }
}

private open class TypeProcessor {
    open fun format(type: Type): String = buildString {
        append(type.shortName)

        if (type is TypeRef && type.genericArgs.isNotEmpty()) {
            type.genericArgs.joinToString(", ", "<", ">", transform = ::format).let(::append)
        }

        if (type.nullable) append('?')
    }
}

private class ImportingTypeProcessor(private val pkg: String, private val imports: ImportDirectives) : TypeProcessor() {
    override fun format(type: Type): String = buildString {
        if (type is TypeRef && type.pkg != pkg && type.pkg != "kotlin") {
            val existingImport = imports[type.baseName]

            if (existingImport == null) {
                imports += ImportDirective("${type.pkg}.${type.baseName}")
            } else if (existingImport.fullName != "${type.pkg}.${type.baseName}") {
                append(type.pkg)
                append('.')
            }
        }

        append(super.format(type))
    }
}
