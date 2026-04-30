/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.dynamodb

import aws.sdk.kotlin.codegen.sdkId
import aws.smithy.kotlin.codegen.KotlinSettings
import aws.smithy.kotlin.codegen.integration.AppendingSectionWriter
import aws.smithy.kotlin.codegen.integration.KotlinIntegration
import aws.smithy.kotlin.codegen.integration.SectionWriterBinding
import aws.smithy.kotlin.codegen.lang.KotlinTypes
import aws.smithy.kotlin.codegen.rendering.ServiceClientGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

private val DYNAMODB_SERVICES = setOf("dynamodb", "dynamodb streams")

/**
 * Generates `defaultMaxAttempts` and `defaultInitialDelay` overrides in the companion object
 * for DynamoDB and DynamoDB Streams services, as required by the New Retry Behavior.
 */
class DynamoDbRetryDefaultsIntegration : KotlinIntegration {
    companion object {
        /** DynamoDB default max attempts as defined in the New Retry Behavior. */
        const val DYNAMODB_MAX_ATTEMPTS = 4

        /** DynamoDB default initial delay in milliseconds as defined in the New Retry Behavior. */
        const val DYNAMODB_INITIAL_DELAY_MS = 25
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model.expectShape(settings.service, ServiceShape::class.java).sdkId.lowercase() in DYNAMODB_SERVICES

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                ServiceClientGenerator.Sections.CompanionObject,
                AppendingSectionWriter { writer ->
                    writer.write("")
                    writer.write("override val defaultMaxAttempts: Int = #L", DYNAMODB_MAX_ATTEMPTS)
                    writer.write(
                        "override val defaultInitialDelay: #T = #L.#T",
                        KotlinTypes.Time.Duration,
                        DYNAMODB_INITIAL_DELAY_MS,
                        KotlinTypes.Time.milliseconds,
                    )
                },
            ),
        )
}
