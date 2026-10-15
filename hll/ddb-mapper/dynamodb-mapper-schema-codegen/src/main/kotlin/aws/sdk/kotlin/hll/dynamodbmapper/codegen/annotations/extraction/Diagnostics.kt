/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.core.CodegenLogger
import aws.sdk.kotlin.hll.codegen.core.SymbolRef

/**
 * The severity of a [Diagnostic]
 */
internal enum class DiagnosticSeverity { WARNING, ERROR }

/**
 * A problem found while extracting the IR from annotated classes
 * @param severity How severe the problem is. Any [DiagnosticSeverity.ERROR] fails the build.
 * @param message The user-facing message
 * @param symbol The source symbol the problem pertains to, if any
 */
internal data class Diagnostic(
    val severity: DiagnosticSeverity,
    val message: String,
    val symbol: SymbolRef?,
)

/**
 * Accumulates [Diagnostic]s during extraction so that a single pass reports every problem, in the order found, rather
 * than failing on the first.
 */
internal class DiagnosticCollector {
    private val _diagnostics = mutableListOf<Diagnostic>()

    /**
     * The diagnostics reported so far, in the order they were reported
     */
    val diagnostics: List<Diagnostic>
        get() = _diagnostics.toList()

    /**
     * Whether any [DiagnosticSeverity.ERROR] has been reported
     */
    val hasErrors: Boolean
        get() = _diagnostics.any { it.severity == DiagnosticSeverity.ERROR }

    /**
     * The number of errors reported so far. Useful for detecting whether a sub-step reported any.
     */
    val errorCount: Int
        get() = _diagnostics.count { it.severity == DiagnosticSeverity.ERROR }

    fun error(message: String, symbol: SymbolRef? = null) {
        _diagnostics += Diagnostic(DiagnosticSeverity.ERROR, message, symbol)
    }

    fun warn(message: String, symbol: SymbolRef? = null) {
        _diagnostics += Diagnostic(DiagnosticSeverity.WARNING, message, symbol)
    }

    /**
     * Emits all accumulated diagnostics to [logger], in the order they were reported
     */
    fun flushTo(logger: CodegenLogger) {
        _diagnostics.forEach { diagnostic ->
            when (diagnostic.severity) {
                DiagnosticSeverity.WARNING -> logger.warn(diagnostic.message, diagnostic.symbol)
                DiagnosticSeverity.ERROR -> logger.error(diagnostic.message, diagnostic.symbol)
            }
        }
    }
}
