/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

import aws.sdk.kotlin.runtime.InternalSdkApi

private val codepointMap = buildMap {
    (0..255).filter(Character::isISOControl).forEach { code -> put(code, unicodeEscape(code)) }
    put(8, "\\b")
    put(9, "\\t")
    put(10, "\\n")
    put(13, "\\r")
    put(34, "\\\"")
    put(36, "\\\$")
    put(92, "\\\\")
}.withDefault(Character::toString)

private fun unicodeEscape(code: Int): String {
    require(code < 65536) { "This function only works for codepoints < 65536. If you've run into this error, fix me!" }
    return "\\u" + String.format("%04X", code)
}

/**
 * Escape a string to one that is safe for use as a string literal by replacing various reserved characters with escape
 * sequences
 */
@InternalSdkApi
public fun String.escape(): String = buildString {
    this@escape.codePoints().forEach { append(codepointMap.getValue(it)) }
}

/**
 * Returns a string with the first letter capitalized (if applicable)
 */
@InternalSdkApi
public val String.capitalizeFirstChar: String
    get() = replaceFirstChar { it.uppercaseChar() }

/**
 * Returns a string with the first letter lowercased (if applicable)
 */
@InternalSdkApi
public val String.lowercaseFirstChar: String
    get() = replaceFirstChar { it.lowercaseChar() }

/**
 * Returns a string with the first letter uppercased (if applicable)
 */
@InternalSdkApi
public val String.uppercaseFirstChar: String
    get() = replaceFirstChar { it.uppercaseChar() }

/**
 * Escapes and quotes a string such that it could be used in codegen
 */
@InternalSdkApi
public fun String.quote(): String = "\"${escape()}\""
