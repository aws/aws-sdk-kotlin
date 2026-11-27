/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.ksp

import aws.sdk.kotlin.hll.codegen.core.CodegenLogger
import aws.sdk.kotlin.hll.codegen.core.SymbolRef
import aws.sdk.kotlin.runtime.InternalSdkApi
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

/**
 * A [CodegenLogger] backed by a [KSPLogger]. Symbols created by [asSymbolRef] are unwrapped back to their [KSNode] so
 * KSP can locate each message in the user's source.
 * @param delegate The underlying KSP logger
 */
@InternalSdkApi
public class KspCodegenLogger(private val delegate: KSPLogger) : CodegenLogger {
    override fun logging(message: String, symbol: SymbolRef?): Unit = delegate.logging(message, symbol?.node)
    override fun info(message: String, symbol: SymbolRef?): Unit = delegate.info(message, symbol?.node)
    override fun warn(message: String, symbol: SymbolRef?): Unit = delegate.warn(message, symbol?.node)
    override fun error(message: String, symbol: SymbolRef?): Unit = delegate.error(message, symbol?.node)
    override fun exception(e: Throwable): Unit = delegate.exception(e)
}

/**
 * Wraps this node as an opaque [SymbolRef] for use with a [CodegenLogger]
 */
@InternalSdkApi
public fun KSNode.asSymbolRef(): SymbolRef = KspSymbolRef(this)

private data class KspSymbolRef(val node: KSNode) : SymbolRef {
    override fun toString(): String = node.toString()
}

private val SymbolRef.node: KSNode?
    get() = (this as? KspSymbolRef)?.node
