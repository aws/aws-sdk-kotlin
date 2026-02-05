/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi

internal fun charSet(chars: String) = chars.toCharArray().toSet()
internal fun charSet(range: CharRange) = range.toSet()

private object Rfc3986CharSets {
    val alpha = charSet('A'..'Z') + charSet('a'..'z')
    val digit = charSet('0'..'9')
    val unreserved = alpha + digit + charSet("-._~")
    val hexdig = digit + charSet('A'..'F')
    val pctEncoded = hexdig + '%'
    val subDelims = charSet("!$&'()*+,;=")
    val regName = unreserved + pctEncoded + subDelims
}

/**
 * Determines if the given region is valid for the purposes of endpoint lookup, specifically that the region is suitable
 * to use in a URI hostname according to [RFC 3986 § 3.2.2](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.2).
 *
 * Valid characters for regions include:
 * * URI unreserved characters:
 *   * Uppercase letters (`A` through `Z`)
 *   * Lowercase letters (`a` through `z`)
 *   * Digits (`0` through `9`)
 *   * Hyphen (`-`)
 *   * Period/dot (`.`)
 *   * Tilde (`~`)
 *   * Underscore (`_`)
 * * Percent (`%`)
 * * URI sub-delimiters
 *   * Ampersand (`&`)
 *   * Apostrophe (`'`)
 *   * Asterisk (`*`)
 *   * Comma (`,`)
 *   * Dollar sign (`$`)
 *   * Equals sign (`=`)
 *   * Exclamation point (`!`)
 *   * Parentheses (`(` and `)`)
 *   * Plus (`+`)
 *   * Semicolon (`;`)
 *
 * Notable characters which are _invalid_ for regions include:
 * * Space (` `)
 * * At sign (`@`)
 * * Backtick/grave (`` ` ``)
 * * Braces (`{` and `}`)
 * * Brackets (`[` and `]`)
 * * Caret (`^`)
 * * Colon (`:`)
 * * Double quote (`"`)
 * * Hash/number sign (`#`)
 * * Inequality signs (`<` and `>`)
 * * Pipe (`|`)
 * * Question mark (`?`)
 * * Slashes (`/` and `\`)
 * * All non-ASCII characters (e.g., Unicode characters)
 */
@InternalSdkApi
public fun isRegionValid(region: String): Boolean = region.isNotEmpty() && region.all(Rfc3986CharSets.regName::contains)

/**
 * Validates that a region is suitable to use in a URI hostname according to
 * [RFC 3986 § 3.2.2](https://www.rfc-editor.org/rfc/rfc3986#section-3.2.2). See [isRegionValid] for a detailed
 * description of the validation criteria.
 */
@InternalSdkApi
public fun validateRegion(region: String): String = region.also {
    if (!isRegionValid(region)) {
        throw ConfigurationException("""Configured region "$region" is invalid. A region must be a valid URI host component.""")
    }
}
