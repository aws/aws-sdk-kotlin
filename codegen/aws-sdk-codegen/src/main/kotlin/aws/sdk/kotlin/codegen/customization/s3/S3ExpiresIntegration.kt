/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.core.KotlinWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.model.*
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import aws.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.DeprecatedTrait
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.traits.OutputTrait
import software.amazon.smithy.model.transform.ModelTransformer
import kotlin.streams.asSequence

/**
 * An integration used to customize behavior around S3's members named `Expires`.
 */
class S3ExpiresIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) = model.expectShape<ServiceShape>(settings.service).isS3 && model.shapes<OperationShape>().any { it.hasExpiresMember(model) }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()

        // Ensure all `Expires` shapes are timestamps
        val expiresShapeTimestampMap = model.shapes()
            .asSequence()
            .mapNotNull { shape ->
                shape.members()
                    .singleOrNull { member -> member.memberName.equals("Expires", ignoreCase = true) }
                    ?.target
            }
            .associateWith { ShapeType.TIMESTAMP }

        var transformedModel = transformer.changeShapeType(model, expiresShapeTimestampMap)

        // Add an `ExpiresString` string shape to the model
        val expiresString = StringShape.builder()
        expiresString.id("aws.sdk.kotlin.s3.synthetic#ExpiresString")
        transformedModel = transformedModel.toBuilder().addShape(expiresString.build()).build()

        // For output shapes only, deprecate `Expires` and add a synthetic member that targets `ExpiresString`
        return transformer.mapShapes(transformedModel) { shape ->
            if (shape.hasTrait<OutputTrait>() && shape.memberNames.any { it.equals("Expires", ignoreCase = true) }) {
                val builder = (shape as StructureShape).toBuilder()

                // Deprecate `Expires`
                val expiresMember = shape.members().single { it.memberName.equals("Expires", ignoreCase = true) }

                builder.removeMember(expiresMember.memberName)
                val deprecatedTrait = DeprecatedTrait.builder()
                    .message("Please use `expiresString` which contains the raw, unparsed value of this field.")
                    .build()

                builder.addMember(
                    expiresMember.toBuilder()
                        .addTrait(deprecatedTrait)
                        .build(),
                )

                // Add a synthetic member targeting `ExpiresString`
                val expiresStringMember = MemberShape.builder()
                expiresStringMember.target(expiresString.id)
                expiresStringMember.id(expiresMember.id.toString() + "String") // i.e. com.amazonaws.s3.<MEMBER_NAME>$ExpiresString
                expiresStringMember.addTrait(HttpHeaderTrait("ExpiresString")) // Add HttpHeaderTrait to ensure the field is deserialized
                expiresMember.getTrait<DocumentationTrait>()?.let {
                    expiresStringMember.addTrait(it) // Copy documentation from `Expires`
                }
                builder.addMember(expiresStringMember.build())
                builder.build()
            } else {
                shape
            }
        }
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + applyExpiresFieldInterceptor

    internal val applyExpiresFieldInterceptor = object : ProtocolMiddleware {
        override val name: String = "ExpiresFieldInterceptor"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = ctx.model.expectShape<ServiceShape>(ctx.settings.service).isS3 && op.hasExpiresMember(ctx.model)

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val interceptorSymbol = buildSymbol {
                name = "ExpiresFieldInterceptor"
                namespace = ctx.settings.pkg.subpackage("internal")
            }

            writer.write("op.interceptors.add(#T)", interceptorSymbol)
        }
    }

    private fun OperationShape.hasExpiresMember(model: Model): Boolean {
        val input = model.expectShape(this.inputShape)
        val output = model.expectShape(this.outputShape)

        return (input.memberNames + output.memberNames).any {
            it.equals("Expires", ignoreCase = true)
        }
    }
}
