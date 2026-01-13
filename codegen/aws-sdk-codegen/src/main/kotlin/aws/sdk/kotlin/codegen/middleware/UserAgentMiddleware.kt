/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.model.buildSymbol
import aws.smithy.kotlin.codegen.model.namespace
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Middleware that sets the User-Agent and x-amz-user-agent headers
 */
class UserAgentMiddleware : ProtocolMiddleware {
    override val name: String = "UserAgent"
    override val order: Byte = 20

    private val uaSymbol = buildSymbol {
        name = "AwsUserAgentMetadata"
        namespace(AwsKotlinDependency.AWS_HTTP)
    }

    private val apiMetaSymbol = buildSymbol {
        name = "ApiMetadata"
        namespace(AwsKotlinDependency.AWS_HTTP)
    }
    private val middlewareSymbol = buildSymbol {
        name = "UserAgent"
        namespace(AwsKotlinDependency.AWS_HTTP, subpackage = "middleware")
    }

    override fun renderProperties(writer: KotlinWriter) {
        // static metadata that doesn't change per/request
        writer.addImport(uaSymbol)
        writer.addImport(apiMetaSymbol)
        writer.addImport(middlewareSymbol)
        writer.write(
            "private val awsUserAgentMetadata = #T.fromEnvironment(#T(ServiceId, SdkVersion), config.applicationId)",
            uaSymbol,
            apiMetaSymbol,
        )
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.install(#T(awsUserAgentMetadata))", middlewareSymbol)
    }
}
