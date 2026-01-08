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
     * Pair of TTL field name to lifetime in seconds, used to support the [DynamoDbTtlSeconds] annotation
     */
    public val TtlField: AttributeKey<Pair<String, Long>> = AttributeKey("aws.sdk.kotlin.hll.dynamodbmapper#TtlField")

    /**
     * Set of field names annotated with [DynamoDbCounter]
     */
    public val CounterFields: AttributeKey<Set<String>> = AttributeKey("aws.sdk.kotlin.hll.dynamodbmapper#CounterFields")
}
