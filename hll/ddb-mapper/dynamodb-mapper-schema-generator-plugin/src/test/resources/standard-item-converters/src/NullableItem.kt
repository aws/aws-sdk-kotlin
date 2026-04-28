/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.smithy.kotlin.runtime.time.Instant

@DynamoDbItem
public data class NullableItem(
    @DynamoDbPartitionKey var id: Int,

    /**
     * A selection of nullable types
     */
    var string: String?,
    var byte: Byte?,
    var int: Int?,
    var instant: Instant?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NullableItem) return false

        if (id != other.id) return false
        if (string != other.string) return false
        if (byte != other.byte) return false
        if (int != other.int) return false
        if (instant?.epochSeconds != other.instant?.epochSeconds) return false

        return true
    }
}
