/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * A mutable collection of [ImportDirective] entries for eventually writing to a code generator. Imports within this
 * collection may be queried by their full-qualified name (e.g., `com.foo.bar.Baz`) or by their "short" name (e.g.,
 * `Baz` or `BazAlias`).
 */
@InternalSdkApi
public class ImportDirectives : MutableSet<ImportDirective> by mutableSetOf() {
    /**
     * Find a matching [ImportDirective] by its short name, which may be the normal unqualified type name or an import
     * alias
     * @param shortName The short name by which to search
     */
    public fun getByShortName(shortName: String): ImportDirective? = find { it.shortName == shortName }

    /**
     * Find a matching [ImportDirective] by its full name
     * @param fullName The full name by which to search
     */
    public fun getByFullName(fullName: String): ImportDirective? = find { it.fullName == fullName }

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
     * Initializes a new import directive
     * @param type The type reference to import
     * @param alias An optional alias by which to refer to the type in code
     */
    @InternalSdkApi
    public constructor(type: TypeRef, alias: String? = null) : this(type.fullName, alias)

    private val aliasFormatted = alias?.let { " as $it" } ?: ""

    /**
     * The formatted `import` code string for this directive
     */
    public val formatted: String = "import $fullName$aliasFormatted"

    /**
     * The "short name" of an import directive that can be used in code. This is [alias] (if not null) or the last
     * segment of the full name (e.g., the last segment of `foo.bar.Baz` is `Baz`).
     */
    public val shortName: String = alias ?: fullName.split(".").last()
}
