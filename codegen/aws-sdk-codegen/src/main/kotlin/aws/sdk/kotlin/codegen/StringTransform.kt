/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.utils.toPascalCase

private val whitespaceRegex = Regex("\\s")

private val spaceOrDashRegex = Regex("\\s|-")

/**
 * Base interface for string transformers.
 */
fun interface StringTransformer {
    fun transform(input: String): String
}

/**
 * Implements all standardized sdkId transforms.
 */
object SdkIdTransformers {
    // Replace all whitespace from the sdkId with underscores and lowercase all letters.
    val LowerSnakeCase = StringTransformer { it.replaceWhitespace("_").lowercase() }

    // None. Directly use the sdkId.
    val Identity = StringTransformer { it }

    // Remove all whitespace from the sdkId.
    val NoWhitespace = StringTransformer { it.replaceWhitespace("") }

    // Replace all whitespace from the sdkId with dashes and lowercase all letters.
    val LowerKebabCase = StringTransformer { it.replaceWhitespace("-").lowercase() }

    // Replace all whitespace from the sdkId with underscores and capitalize all letters.
    val UpperSnakeCase = StringTransformer { it.replaceWhitespace("_").uppercase() }
}

/**
 * Implements all standardized SigV4 service signing name transforms.
 */
object SigV4NameTransformers {
    // Replace all dashes from the SigV4 service signing name with underscores and capitalize all letters.
    val UpperSnakeCase = StringTransformer { it.replaceSpaceOrDash("_").uppercase() }

    // Remove dashes and convert SigV4 service signing name to PascalCase
    val PascalCase = StringTransformer { it.toPascalCase() }
}

/**
 * Applies the given transformer to the string.
 */
fun <T : StringTransformer> String.withTransform(transformer: T): String = transformer.transform(this)

private fun String.replaceWhitespace(replacement: String) = replace(whitespaceRegex, replacement)

private fun String.replaceSpaceOrDash(replacement: String) = replace(spaceOrDashRegex, replacement)
