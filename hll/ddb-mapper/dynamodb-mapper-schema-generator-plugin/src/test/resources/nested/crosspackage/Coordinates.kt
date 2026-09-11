/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example.geo

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable

@DynamoDbMappable
public data class Coordinates(
    var latitude: Double,
    var longitude: Double,
)
