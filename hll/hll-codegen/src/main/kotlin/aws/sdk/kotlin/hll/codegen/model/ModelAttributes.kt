/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Defines [AttributeKey] instances that relate to the data model of low-level to high-level codegen
 */
@InternalSdkApi
public object ModelAttributes {
    /**
     * The types involved for DSL-style method(s) for working with a complex member, if applicable
     */
    public val Dsls: AttributeKey<List<DslInfo>> = AttributeKey("aws.sdk.kotlin.hll#Dsls")

    /**
     * For a given high-level [Member], this attribute key identifies the associated low-level [Member]
     */
    public val LowLevelMember: AttributeKey<Member> = AttributeKey("aws.sdk.kotlin.hll#LowLevelMember")

    /**
     * For a given high-level [Operation], this attribute key identifies the associated low-level [Operation]
     */
    public val LowLevelOperation: AttributeKey<Operation> = AttributeKey("aws.sdk.kotlin.hll#LowLevelOperation")

    /**
     * For a given high-level [Structure], this attribute key identifies the associated low-level [Structure]
     */
    public val LowLevelStructure: AttributeKey<Structure> = AttributeKey("aws.sdk.kotlin.hll#LowLevelStructure")
}
