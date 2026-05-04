/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Defines [AttributeKey] instances for schema-specific metadata
 */
public object SchemaAttributes {
    /**
     * Set of TTL fields, each containing field name and lifetime in seconds
     */
    public val TtlFields: AttributeKey<Set<Pair<String, Long>>> = AttributeKey("aws.sdk.kotlin.hll.dynamodbmapper#TtlFields")

    /**
     * Set of field names annotated with [DynamoDbCounter]
     */
    public val CounterFields: AttributeKey<Set<String>> = AttributeKey("aws.sdk.kotlin.hll.dynamodbmapper#CounterFields")
}
