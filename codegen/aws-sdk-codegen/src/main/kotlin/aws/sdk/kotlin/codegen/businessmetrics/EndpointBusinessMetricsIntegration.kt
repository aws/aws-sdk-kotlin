/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.businessmetrics

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointBusinessMetrics

/**
 * Renders the addition of endpoint & endpoint adjacent business metrics.
 */
class EndpointBusinessMetricsIntegration : KotlinIntegration {
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(EndpointBusinessMetrics, endpointBusinessMetricsSectionWriter),
        )

    private val endpointBusinessMetricsSectionWriter = SectionWriter { writer, _ ->
        writer.write("")
        writer.write(
            "if (endpoint.attributes.contains(#T)) request.context.#T(#T.SERVICE_ENDPOINT_OVERRIDE)",
            RuntimeTypes.Core.BusinessMetrics.ServiceEndpointOverride,
            RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
            RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
        )
        writer.write(
            "if (endpoint.attributes.contains(#T)) request.context.#T(#T.ACCOUNT_ID_BASED_ENDPOINT)",
            RuntimeTypes.Core.BusinessMetrics.AccountIdBasedEndpointAccountId,
            RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
            RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
        )
        writer.write(
            "if (endpoint.attributes.contains(#T.SigningService) && endpoint.attributes[#T.SigningService] == \"s3express\") request.context.#T(#T.S3_EXPRESS_BUCKET)",
            AwsSigningAttributes,
            AwsSigningAttributes,
            RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
            AwsRuntimeTypes.Http.Interceptors.BusinessMetrics.AwsBusinessMetric,
        )
        writer.write(
            "if (request.identity.attributes.contains(#T.AccountId)) request.context.#T(#T.RESOLVED_ACCOUNT_ID)",
            AwsRuntimeTypes.Core.Client.AwsClientOption,
            RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
            RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
        )
        writer.write("")
    }
}
