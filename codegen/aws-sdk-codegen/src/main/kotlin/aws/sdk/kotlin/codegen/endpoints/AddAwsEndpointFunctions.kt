/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinDelegator
import aws.smithy.kotlin.codegen.core.useFileWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.rendering.endpoints.EndpointCustomization
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.model.node.Node
import java.io.File

internal const val PARTITIONS_JSON_ENV_VAR = "PARTITIONS_FILE"
internal const val PARTITIONS_JSON_SYS_PROP = "aws.partitions_file"
private const val PARTITIONS_RESOURCE = "aws/sdk/kotlin/codegen/partitions.json"

/**
 * Adds support for AWS specific endpoint functions
 */
class AddAwsEndpointFunctions : KotlinIntegration {
    override fun customizeEndpointResolution(ctx: ProtocolGenerator.GenerationContext): EndpointCustomization = object : EndpointCustomization {
        override val externalFunctions: Map<String, Symbol> = mapOf(
            "aws.parseArn" to AwsRuntimeTypes.Endpoint.Functions.parseArn,
            "aws.isVirtualHostableS3Bucket" to AwsRuntimeTypes.Endpoint.Functions.isVirtualHostableS3Bucket,
            "aws.partition" to buildSymbol {
                name = "partition"
                namespace = PartitionsGenerator.getSymbol(ctx.settings).namespace
            },
        )
    }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val partitionsData = getPartitionsJson()
        val partitions = Node.parse(partitionsData).expectObjectNode()
        val partitionsSymbol = PartitionsGenerator.getSymbol(ctx.settings)

        delegator.useFileWriter(partitionsSymbol) {
            PartitionsGenerator(it, partitions).render()
        }
    }
    private fun getPartitionsJson(): String = System.getProperty(PARTITIONS_JSON_SYS_PROP)?.let { File(it).readText() }
        ?: System.getenv(PARTITIONS_JSON_ENV_VAR)?.let { File(it).readText() }
        ?: javaClass.classLoader.getResource(PARTITIONS_RESOURCE)?.readText()
        ?: throw CodegenException("could not load partitions.json resource")
}
