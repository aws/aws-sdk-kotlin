/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.model.MutableItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

internal data class MutableItemImpl(private val delegate: MutableMap<String, AttributeValue>) :
    MutableItem,
    MutableMap<String, AttributeValue> by delegate
