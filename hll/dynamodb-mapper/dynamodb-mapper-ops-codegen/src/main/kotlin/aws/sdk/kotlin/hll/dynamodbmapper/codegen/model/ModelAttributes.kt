/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Defines [AttributeKey] instances that relate to the data model of low-level to high-level codegen
 */
object ModelAttributes {
    /**
     * For a given high-level [Operation], this attribute key identifies the associated low-level [Operation]
     */
    val LowLevelOperation: AttributeKey<Operation> = AttributeKey("aws.sdk.kotlin.ddbmapper#LowLevelOperation")

    /**
     * For a given high-level [Structure], this attribute key identifies the associated low-level [Structure]
     */
    val LowLevelStructure: AttributeKey<Structure> = AttributeKey("aws.sdk.kotlin.ddbmapper#LowLevelStructure")
}
