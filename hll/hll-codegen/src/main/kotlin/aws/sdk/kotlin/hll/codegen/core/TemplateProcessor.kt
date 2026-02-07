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
