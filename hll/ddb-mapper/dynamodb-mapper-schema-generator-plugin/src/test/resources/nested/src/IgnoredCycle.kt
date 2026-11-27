/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbIgnore
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

// The only self-references are through an ignored property and a private property, neither of which is mapped to an
// attribute. This must NOT be treated as a cyclic nesting.
@DynamoDbItem
public data class IgnoredCycle(
    @DynamoDbPartitionKey var id: Int,
    var name: String,
    @DynamoDbIgnore var ignoredParent: IgnoredCycle? = null,
) {
    private var privateParent: IgnoredCycle? = null
}
