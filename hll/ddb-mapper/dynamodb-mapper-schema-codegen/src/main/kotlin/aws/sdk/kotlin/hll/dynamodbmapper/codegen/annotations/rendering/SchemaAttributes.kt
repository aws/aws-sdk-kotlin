/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Internal schema code generation attributes
 */
internal object SchemaAttributes {
    /**
     * Whether a value converter should be generated for the class being processed
     */
    internal val ShouldRenderValueConverterAttribute: AttributeKey<Boolean> = AttributeKey("ShouldRenderValueConverter")
}
