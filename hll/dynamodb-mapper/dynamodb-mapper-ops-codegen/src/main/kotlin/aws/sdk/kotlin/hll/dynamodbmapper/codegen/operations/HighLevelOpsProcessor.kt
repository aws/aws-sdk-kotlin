/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.ksp.processors.HllKspProcessor
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperPkg
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.toHighLevel
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering.HighLevelRenderer
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * The high-level ops KSP processor. This class controls the overall flow of symbol processing, including resolving the
 * symbols to use as codegen input (i.e., the low-level DynamoDB operations/types), wiring up rendering context, and
 * starting the top-level renderer (which may in turn call other renderers).
 */
internal class HighLevelOpsProcessor(environment: SymbolProcessorEnvironment) : HllKspProcessor(environment) {
    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger
    private val opAllowlist = environment.options["op-allowlist"]?.split(";")
    private val pkg = environment.options["pkg"] ?: MapperPkg.Hl.Ops

    private val ctx by lazy {
        val codegenFactory = CodeGeneratorFactory(codeGenerator, logger) // FIXME Pass dependencies
        RenderContext(logger, codegenFactory, pkg, "dynamodb-mapper-ops-codegen")
    }

    override fun processImpl(resolver: Resolver): List<KSAnnotated> {
        logger.info("Scanning low-level DDB client for operations and types")
        val operations = getOperations(resolver)

        logger.info("Rendering high-level operations and types for ${operations.map { it.name }}")
        HighLevelRenderer(ctx, operations).render()
        return listOf()
    }

    private fun allow(func: KSFunctionDeclaration): Boolean {
        val name = func.simpleName.getShortName()
        val allowed = opAllowlist?.contains(name)

        when (allowed) {
            false -> logger.warn("$name not in allowlist; skipping codegen")
            true -> logger.info("$name in allowlist; processing...")
            null -> Unit // There is no allowlist—don't log anything
        }

        return allowed ?: true
    }

    private fun getOperations(resolver: Resolver): List<Operation> = resolver
        .getClassDeclarationByName<DynamoDbClient>()!!
        .getDeclaredFunctions()
        .filter(::allow)
        .map(Operation::from)
        .map { it.toHighLevel(pkg, ctx) }
        .toList()
}
