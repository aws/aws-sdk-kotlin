/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.SerializeInputImpl

/**
 * Defines input to the serialization step of the pipeline
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 */
public interface SerializeInput<T, HReq> {
    /**
     * The high-level request object which is to be serialized into a low-level request object
     */
    public val highLevelRequest: HReq

    /**
     * The [ItemSchema] to use for serializing objects into items
     */
    public val serializeSchema: ItemSchema<T>
}

/**
 * Creates a new [SerializeInput]
 * @param T The type of objects being converted to/from DynamoDB items
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param highLevelRequest The high-level request object which is to be serialized into a low-level request object
 * @param serializeSchema The [ItemSchema] to use for serializing objects into items
 */
public fun <T, HReq> SerializeInput(highLevelRequest: HReq, serializeSchema: ItemSchema<T>): SerializeInput<T, HReq> =
    SerializeInputImpl(highLevelRequest, serializeSchema)
