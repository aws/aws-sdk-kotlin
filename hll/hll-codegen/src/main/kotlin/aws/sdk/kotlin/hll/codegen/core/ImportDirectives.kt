/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A mutable collection of [ImportDirectives] for eventually writing to a code generator
 */
@InternalSdkApi
public class ImportDirectives : MutableSet<ImportDirective> by mutableSetOf() {
    public operator fun get(shortName: String): ImportDirective? = firstOrNull { it.shortName == shortName }

    /**
     * Returns a formatted code string with each import on a dedicated line. Imports will be sorted with the following
     * precedence:
     * 1. Unaliased imports before alised imports
     * 2. The special package prefixes `java`, `javax`, `kotlin` after all other imports
     * 3. Lexicographically sorted
     */
    public val formatted: String
        get() = buildString {
            sortedWith(importComparator).forEach { appendLine(it.formatted) }
        }
}

private val specialPrefixes = setOf(
    "java.",
    "javax.",
    "kotlin.",
)

private val importComparator = compareBy<ImportDirective> { it.alias != null } // aliased imports at the very end
    .thenBy { directive -> specialPrefixes.any { directive.fullName.startsWith(it) } } // special prefixes < aliases
    .thenBy { it.fullName } // sort alphabetically

/**
 * Describes a Kotlin `import` directive
 * @param fullName The full name of the import (e.g., `java.net.Socket`)
 * @param alias An optional alias for the import (e.g., `JavaSocket`). If present, a formatted code string for this
 * directive will include an `as` clause (e.g., `import java.net.Socket as JavaSocket`).
 */
@InternalSdkApi
public data class ImportDirective(val fullName: String, val alias: String? = null) {
    /**
     * The unaliased "short name" of an import directive—namely, everything after the last `.` separator. For example,
     * for the full name `java.net.Socket` the short name is `Socket`.
     */
    public val shortName: String = fullName.split(".").last()

    private val aliasFormatted = alias?.let { " as $it" } ?: ""

    /**
     * The formatted `import` code string for this directive
     */
    public val formatted: String = "import $fullName$aliasFormatted"
}

@InternalSdkApi
public fun ImportDirective(type: TypeRef, alias: String? = null): ImportDirective = ImportDirective(type.fullName, alias)
