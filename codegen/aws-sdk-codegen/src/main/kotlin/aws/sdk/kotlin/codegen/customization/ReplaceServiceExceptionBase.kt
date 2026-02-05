/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.smithy.kotlin.codegen.core.CodegenContext
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.core.declareSection
import aws.smithy.kotlin.codegen.core.getContextValue
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionId
import aws.smithy.kotlin.codegen.integration.SectionWriter
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import aws.smithy.kotlin.codegen.rendering.ServiceExceptionBaseClassGenerator

/**
 * Integration that updates the generated service exception base class (e.g. `S3Exception`, `DynamoDbException`, etc)
 * to inherit from `AwsServiceException` instead of just `ServiceException`.
 */
class ReplaceServiceExceptionBase : KotlinIntegration {
    // S3 further customizes this by overriding the error metadata. See [aws.sdk.kotlin.codegen.customization.s3.S3ErrorMetadataIntegration]
    override val order: Byte = -10
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(ExceptionBaseClassGenerator.ExceptionBaseClassSection, exceptionSectionWriter))

    private val exceptionSectionWriter = SectionWriter { writer, _ ->
        val ctx = writer.getContextValue(CodegenContext.Key)
        AwsServiceExceptionBaseClassGenerator().render(ctx, writer)
    }
}

class AwsServiceExceptionBaseClassGenerator : ServiceExceptionBaseClassGenerator(AwsRuntimeTypes.Core.AwsServiceException) {
    object Sections {
        object RenderExtra : SectionId
    }
    override fun renderExtra(ctx: CodegenContext, writer: KotlinWriter) {
        writer.declareSection(Sections.RenderExtra)
    }
}
