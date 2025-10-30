/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.codegen

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.ksp.processors.HllKspProcessor
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.conversionMappings
import aws.sdk.kotlin.hll.s3transfermanager.codegen.mappings.ioMappings
import aws.sdk.kotlin.hll.s3transfermanager.codegen.renderers.ConversionRenderer
import aws.sdk.kotlin.hll.s3transfermanager.codegen.renderers.IORenderer
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

internal class S3TransferManagerSymbolProcessor(environment: SymbolProcessorEnvironment) : HllKspProcessor(environment) {
    val rendererName = "s3-transfer-manager-code-generator"
    val codeGenerator = environment.codeGenerator
    val logger = environment.logger

    override fun processImpl(resolver: Resolver): List<KSAnnotated> {
        val ioMappingsContext =
            RenderContext(
                logger,
                CodeGeneratorFactory(codeGenerator, logger),
                "aws.sdk.kotlin.hll.s3transfermanager.model",
                rendererName,
            )

        ioMappings.forEach { mapping ->
            IORenderer(
                ioMappingsContext,
                mapping.className,
                mapping,
                resolver,
            ).render()
        }

        val conversionMappingsContext =
            RenderContext(
                logger,
                CodeGeneratorFactory(codeGenerator, logger),
                "aws.sdk.kotlin.hll.s3transfermanager.model.utils",
                rendererName,
            )

        ConversionRenderer(
            conversionMappingsContext,
            "Converters", // TODO: Will this override the file after each conversion ?
            conversionMappings,
            resolver,
        ).render()

        return listOf()
    }
}
