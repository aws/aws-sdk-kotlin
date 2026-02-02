/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.profile

/**
 * Encapsulates the content and position of a line within some file.
 */
internal data class FileLine(val lineNumber: Int, val content: String)

/**
 * Matches a profile declaration line, e.g.:
 * - `[profile foo]`
 * - `[default]`
 * - `[profile default]`
 * - `[profile f oo]`    (recognized profile declaration, but omitted in the final config)
 * - `[foo]`             (recognized profile declaration, may be omitted depending on file type)
 */
internal fun FileLine.isProfile(): Boolean = isSection() && !isSsoSession()

/**
 * Matches a sso-session declaration line, e.g.:
 * - `[sso-session mysession]`
 */
internal fun FileLine.isSsoSession(): Boolean = isSection() && content.contains(Literals.SSO_SESSION_KEYWORD)

/**
 * Matches a section declaration line, e.g.:
 * - `[profile foo]`
 * - `[default]`
 * - `[sso-session my-session]`
 */
internal fun FileLine.isSection(): Boolean = content.stripComments().trim().let {
    it.startsWith(Literals.SECTION_PREFIX) && it.endsWith(Literals.SECTION_SUFFIX)
}

/**
 * Matches a NON-inline comment line, e.g.:
 * - `; a comment`
 * - `# a comment`
 */
internal fun FileLine.isComment() = content.trim().let {
    it.startsWith(Literals.COMMENT_1) || it.startsWith(Literals.COMMENT_2)
}

/**
 * Matches a property definition line, e.g.:
 * - `ident = val`
 * - `ident = val  ` (trailing whitespace)
 * - `ident =`       (can either be a blank property, or if a child declaration follows, a parent that declares itself an object)
 * - `id ent =`      (a recognized property declaration, but omitted in the final config)
 */
internal fun FileLine.isProperty() = !content.first().isWhitespace() && content.isProperty()

/**
 * Matches a continuation line, which may or may not also be a sub-property definition (see [isSubProperty]).
 */
internal fun FileLine.isContinuation() = content.first().isWhitespace() && content.substring(1).isNotBlank()

/**
 * Matches a sub-property definition line, e.g.:
 * - `child_ident = val `
 * - `child_ident=val`
 * - `child_ident = val ; failed comment attempt ` (value is "val ; failed comment attempt")
 */
internal fun FileLine.isSubProperty(): Boolean = content.first().isWhitespace() && content.trimStart().isProperty()

private fun String.isProperty() = contains(Literals.PROPERTY_SPLITTER) &&
    split(Literals.PROPERTY_SPLITTER, limit = 2).let {
        it.size == 2 && it[0].trim().isNotEmpty()
    }
