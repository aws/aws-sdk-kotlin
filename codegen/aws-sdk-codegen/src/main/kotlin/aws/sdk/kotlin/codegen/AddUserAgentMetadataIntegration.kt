/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.withBlock
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.OperationShape
import java.io.File

internal const val ADD_USER_AGENT_METADATA_ENV_VAR = "ADD_USER_AGENT_METADATA"
internal const val ADD_USER_AGENT_METADATA_SYS_PROP = "aws.user_agent.add_metadata"

/**
 * Adds additional metadata to the user agent sent in requests. This additional metadata is loaded from a JSON file
 * during the **:codegen:sdk:bootstrap** task. The file name is detected by one of the following means:
 *
 * * The `ADD_USER_AGENT_METADATA` environment variable
 * * The `aws.user_agent.add_metadata` project property. This may be set via:
 *     * The Gradle command line argument `-Paws.user_agent.add_metadata=<path>` –or–
 *     * A line in the **local.properties** file
 *
 * If neither option is configured, no additional metadata is added to the user agent.
 */
class AddUserAgentMetadataIntegration : KotlinIntegration {
    private val metadataFile: String? by lazy {
        System.getenv(ADD_USER_AGENT_METADATA_ENV_VAR) ?: System.getProperty(ADD_USER_AGENT_METADATA_SYS_PROP)
    }

    private val metadata by lazy {
        val json = metadataFile?.let { File(it).readText() } ?: "{}" // Default to empty JSON object (no extra metadata)
        val node = Node.parse(json).expectObjectNode()
        node.members.entries.associate { (k, v) -> k.value to v.expectStringNode().value }
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + addUserAgentMetadataMiddleware

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = metadataFile != null

    private val addUserAgentMetadataMiddleware = object : ProtocolMiddleware {
        override val name: String = "AddUserAgentMetadata"

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.write(
                "op.interceptors.add(#T(extraMetadata))",
                AwsRuntimeTypes.Http.Interceptors.AddUserAgentMetadataInterceptor,
            )
        }

        override fun renderProperties(writer: KotlinWriter) {
            writer.withBlock("private val extraMetadata: Map<String, String> = mapOf(", ")") {
                metadata.forEach { (k, v) -> writer.write("#S to #S,", k, v) }
            }
        }
    }
}
