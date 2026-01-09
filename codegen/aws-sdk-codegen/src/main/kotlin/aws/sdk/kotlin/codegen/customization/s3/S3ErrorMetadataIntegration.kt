/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.customization.AwsServiceExceptionBaseClassGenerator
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.*
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Generates an S3-specific subclass of AwsErrorMetadata.
 */
class S3ErrorMetadataIntegration : KotlinIntegration {

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(AwsServiceExceptionBaseClassGenerator.Sections.RenderExtra, addSdkErrorMetadataWriter))

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.useFileWriter("S3ErrorMetadata.kt", "${ctx.settings.pkg.name}.model") { writer ->
            writer.addImport(AwsRuntimeTypes.Core.AwsErrorMetadata)
            writer.addImport(RuntimeTypes.Core.Collections.AttributeKey)

            writer.withBlock("public class S3ErrorMetadata : AwsErrorMetadata() {", "}") {
                writer.withBlock("public companion object {", "}") {
                    writer.write("""public val RequestId2: AttributeKey<String> = AttributeKey("S3:RequestId2")""")
                }

                writer
                    .write("public val requestId2: String?")
                    .indent()
                    .write("get() = attributes.getOrNull(RequestId2)")
                    .dedent()
            }

            writer.dependencies.addAll(KotlinDependency.KOTLIN_TEST.dependencies)
            writer.dependencies.addAll(KotlinDependency.AWS_PROTOCOL_CORE.dependencies)
        }
    }

    // SectionWriter to override the default sdkErrorMetadata for S3's version
    private val addSdkErrorMetadataWriter = AppendingSectionWriter { writer ->
        writer.write("override val sdkErrorMetadata: S3ErrorMetadata = S3ErrorMetadata()")
    }
}
