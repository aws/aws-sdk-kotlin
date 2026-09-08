/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A string processing engine that replaces template parameters with passed arguments to form a final string. String
 * templates take the form of string literals containing zero or more **parameters**, which are special sequences
 * starting with `#`. When the engine encounters these parameters, it endeavors to replace the entire parameter with an
 * argument value by using a matching [TemplateProcessor]. This may fail if no matching processor is found, there is a
 * mismatch between the template and passed arguments, if data types of arguments are incorrect, or other reasons, in
 * which case an exception will be thrown.
 *
 * # Parameters
 *
 * All parameters start with `#` and end with an uppercase letter (`[A-Z]`). They may optionally contain a number as
 * well, which makes them **Positional Parameters**. Otherwise, they are **Sequential Parameters**. Templates cannot
 * contain _both_ types of parameters, otherwise an exception will be thrown.
 *
 * ## Sequential Parameters
 *
 * Parameters in the form of `#[A-Z]` are sequential parameters. The first such sequential parameter will be substituted
 * with the first passed argument, the second will be replaced with the second argument, and so forth. If the number of
 * sequential parameters does not match the number of arguments, an exception will be thrown.
 *
 * Note that with this type of parameter, substituting the same argument value multiple times requires passing that
 * argument multiple times.
 *
 * ## Positional Parameters
 *
 * Parameters in the form of `#n[A-Z]` (where `n > 0`) are positional parameters. The number `n` in the parameter
 * indicates the 1-based index of the argument to use for substitution. If the number `n` references an index outside
 * the range of arguments (e.g., less than `0` or greater than the number of passed arguments), an exception will be
 * thrown.
 *
 * Note that with this type of parameter, substituting the same argument value multiple times does not require _passing_
 * that argument multiple times, merely using the same `n` value for multiple parameters.
 *
 * # Processors
 *
 * Template engines reference zero or more [TemplateProcessor] instances which perform mapping from raw argument values
 * to strings to be used in parameter substitution. When the template engine encounters a parameter in a string
 * template, it attempts to match the parameter **key** (an uppercase letter `[A-Z]`) to the key of a processor. If no
 * referenced processor has a matching key, an exception will be thrown.
 */
@InternalSdkApi
public class TemplateEngine(processors: List<TemplateProcessor>) {
    private val processors = processors.associate { it.key to it.handler }
    private val parameterRegex = """(?<!#)#([1-9][0-9]*)?([A-Z])""".toRegex()

    /**
     * Process the given [template] by replacing any contained parameters with the values in [args]
     * @param template A string template that contains zero or more parameters
     * @param args A collection of arguments to use in parameter substitution
     * @return A string with all parameters replaced
     */
    public fun process(template: String, args: List<Any>): String {
        var allIndexed: Boolean? = null

        return parameterRegex.replaceIndexed(template) { pos, result ->
            val explicitIndex = result.groupValues[1].takeUnless { it.isEmpty() }?.toInt()?.minus(1)
            val indexed = explicitIndex != null

            fun req(condition: Boolean, message: () -> String) {
                require(condition) {
                    buildString {
                        append("Error in template '")
                        append(template)
                        append("' for parameter '")
                        append(result.value)
                        append("' at index ")
                        append(result.range.first)
                        append(": ")

                        append(message())
                    }
                }
            }

            fun <T> reqNotNull(element: T?, message: () -> String): T {
                req(element != null, message)
                return element!!
            }

            req(allIndexed == null || allIndexed == indexed) {
                "Cannot mix indexed and non-indexed parameters in the same template"
            }
            allIndexed = indexed

            val key = result.groupValues[2].toCharArray().single()
            val handler = reqNotNull(processors[key]) { "No processor found for key character '$key'" }

            val index = explicitIndex ?: pos
            req(index in args.indices) {
                buildString {
                    if (indexed) {
                        append("Index ")
                        append(result.groupValues[1])
                    } else {
                        append("Parameter index ")
                        append(pos)
                    }
                    append(" is outside of args bounds ")
                    appendLine(args.indices)
                    appendLine("""Full template: "$template"""")
                    appendLine("Args: $args")
                }
            }

            handler(args[index])
        }
    }
}

private fun Regex.replaceIndexed(input: CharSequence, transform: (index: Int, MatchResult) -> CharSequence): String {
    var index = 0
    return replace(input) {
        transform(index, it).also { index++ }
    }
}
