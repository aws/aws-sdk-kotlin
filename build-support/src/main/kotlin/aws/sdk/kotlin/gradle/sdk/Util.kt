/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait

private const val DEPRECATED_SHAPES_CUTOFF_DATE: String = "2023-11-28"

public val REMOVE_DEPRECATED_SHAPES_TRANSFORM: String = """
    {
        "name": "awsSmithyKotlinRemoveDeprecatedShapes",
        "args": {
            "until": "$DEPRECATED_SHAPES_CUTOFF_DATE"
        }
    }
""".trimIndent()

/**
 * Convert an Optional<T> to T?
 */
fun <T> java.util.Optional<T>.orNull(): T? = this.orElse(null)

/**
 * Returns the trait name of the protocol of the service
 */
fun ServiceShape.protocolName(): String = listOf(
    RestJson1Trait.ID,
    RestXmlTrait.ID,
    AwsJson1_0Trait.ID,
    AwsJson1_1Trait.ID,
    AwsQueryTrait.ID,
    Ec2QueryTrait.ID,
    Rpcv2CborTrait.ID,
).first { hasTrait(it) }.name
