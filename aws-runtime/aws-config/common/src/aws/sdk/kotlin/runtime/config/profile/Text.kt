/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.profile

internal object Literals {
    const val DEFAULT_PROFILE = "default"
    const val PROFILE_KEYWORD = "profile"
    const val SSO_SESSION_KEYWORD = "sso-session"
    const val SERVICES_KEYWORD = "services"
    const val COMMENT_1 = "#"
    const val COMMENT_2 = ";"
    const val INLINE_COMMENT_1 = " $COMMENT_1"
    const val INLINE_COMMENT_2 = " $COMMENT_2"
    const val SECTION_PREFIX = '['
    const val SECTION_SUFFIX = ']'
    const val PROPERTY_SPLITTER = '='
}

internal const val HELP_TEXT = "See https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html for file format details."

internal fun contextMessage(message: String, lineNumber: Int? = null): String = buildString {
    append(message)
    lineNumber?.let { append(" on line $lineNumber.") }
    append(" $HELP_TEXT")
}

// what constitutes a "valid" profile/property identifier is not explicitly stated in the SEP (nor is it consistent
// across SDKs) but the one universally rejected component is whitespace
internal fun String.isValidIdentifier(): Boolean = none { it.isWhitespace() }

internal fun String.splitProperty(): Pair<String, String> {
    val kv = split(Literals.PROPERTY_SPLITTER, limit = 2)
    return kv[0].trim() to kv[1].trim()
}

internal fun String.splitWhitespace(limit: Int): List<String> = trim().split(" ", "\t", limit = limit).filter(String::isNotBlank).map(String::trim)

internal fun String.stripComments(): String = stripEnd(Literals.COMMENT_1).stripEnd(Literals.COMMENT_2)

internal fun String.stripInlineComments(): String = stripEnd(Literals.INLINE_COMMENT_1).stripEnd(Literals.INLINE_COMMENT_2)

private fun String.stripEnd(delimiter: String): String = split(delimiter, limit = 2)[0]
