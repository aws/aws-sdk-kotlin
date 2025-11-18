/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Defines the logic for converting between objects and DynamoDB items
 * @param T The type of objects which will be converted
 */
@ExperimentalApi
public typealias ItemConverter<T> = Converter<T, Item>
