/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.DeserializeInputImpl
import aws.sdk.kotlin.services.dynamodb.model.GetItemResponse as LowLevelGetItemResponse

/**
 * Defines input to the deserialization step of the pipeline
 * @param T The type of objects being converted to/from DynamoDB items
 * @param S The type of schema used for conversion
 * @param LRes The type of low-level response object (e.g., [LowLevelGetItemResponse])
 */
public interface DeserializeInput<T, S : ItemSchema<T>, LRes> {
    /**
     * The low-level response which is to be deserialized into a high-level response object
     */
    public val lowLevelResponse: LRes

    /**
     * The [ItemSchema] to use for deserializing items into objects
     */
    public val deserializeSchema: S
}

/**
 * Creates a new [DeserializeInput]
 * @param T The type of objects being converted to/from DynamoDB items
 * @param S The type of schema used for conversion
 * @param LRes The type of low-level response object (e.g., [LowLevelGetItemResponse])
 * @param lowLevelResponse The low-level response which is to be deserialized into a high-level response object
 * @param deserializeSchema The [ItemSchema] to use for deserializing items into objects
 */
public fun <T, S : ItemSchema<T>, LRes> DeserializeInput(
    lowLevelResponse: LRes,
    deserializeSchema: S,
): DeserializeInput<T, S, LRes> = DeserializeInputImpl(lowLevelResponse, deserializeSchema)
