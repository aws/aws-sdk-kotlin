/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

internal data class ItemImpl(private val delegate: Map<String, AttributeValue>) :
    Item,
    Map<String, AttributeValue> by delegate
