/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * An opaque handle to a source symbol, used only to locate a log message. Codegen components which must stay independent
 * of the symbol-processing framework (e.g., renderers and intermediate models) carry this instead of a framework type.
 */
@InternalSdkApi
public interface SymbolRef

/**
 * A logger for codegen events, independent of the underlying symbol-processing framework
 */
@InternalSdkApi
public interface CodegenLogger {
    /**
     * Logs a message at the most verbose level
     * @param message The message to log
     * @param symbol The source symbol to which the message pertains, if any
     */
    public fun logging(message: String, symbol: SymbolRef? = null)

    /**
     * Logs an informational message
     * @param message The message to log
     * @param symbol The source symbol to which the message pertains, if any
     */
    public fun info(message: String, symbol: SymbolRef? = null)

    /**
     * Logs a warning
     * @param message The message to log
     * @param symbol The source symbol to which the message pertains, if any
     */
    public fun warn(message: String, symbol: SymbolRef? = null)

    /**
     * Logs an error. Logging an error fails the build once processing completes.
     * @param message The message to log
     * @param symbol The source symbol to which the message pertains, if any
     */
    public fun error(message: String, symbol: SymbolRef? = null)

    /**
     * Logs an exception
     * @param e The exception to log
     */
    public fun exception(e: Throwable)
}
