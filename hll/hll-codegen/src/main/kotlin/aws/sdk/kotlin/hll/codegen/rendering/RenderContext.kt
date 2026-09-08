/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.model.HasAttributes
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

/**
 * Holds useful context data about code generation. Like many context objects, this is sort of a grab-bag of things
 * which may be useful during various stages.
 * @param logger A logger object for sending events to the console
 * @param codegenFactory A factory that creates code generator instances for specific files
 * @param pkg The Kotlin package for the generated code (e.g., `aws.sdk.kotlin.hll.dynamodbmapper.operations`)
 */
@InternalSdkApi
public data class RenderContext(
    val logger: KSPLogger,
    val codegenFactory: CodeGeneratorFactory,
    val pkg: String,
    val rendererName: String = "aws-sdk-kotlin-hll-codegen",
    override val attributes: Attributes = emptyAttributes(),
) : HasAttributes

public fun RenderContext.logging(message: String, symbol: KSNode? = null): Unit = logger.logging(message, symbol)
public fun RenderContext.info(message: String, symbol: KSNode? = null): Unit = logger.info(message, symbol)
public fun RenderContext.warn(message: String, symbol: KSNode? = null): Unit = logger.warn(message, symbol)
public fun RenderContext.error(message: String, symbol: KSNode? = null): Unit = logger.error(message, symbol)

public fun RenderContext.exception(e: Throwable): Unit = logger.exception(e)
