/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.MutableAttributes

/**
 * Defines [AttributeKey] instances that relate to the data model of low-level to high-level codegen
 */
@InternalSdkApi
public object ModelAttributes {
    /**
     * The types involved for a DSL-style method for working with a complex member, if applicable
     */
    public val DslInfo: AttributeKey<DslInfo> = AttributeKey("aws.sdk.kotlin.hll#DslInfo")

    /**
     * Specifies whether the given API declaration (e.g., method, field, parameter, etc.) is generated.
     */
    public val GeneratedApi: AttributeKey<Boolean> = AttributeKey("aws.sdk.kotlin.hll.codegen#Generated")

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

public val Attributes.generatedApi: Boolean
    get() = this.getOrNull(ModelAttributes.GeneratedApi) ?: false

public var MutableAttributes.generatedApi: Boolean
    get() = this.getOrNull(ModelAttributes.GeneratedApi) ?: false
    set(value) {
        this.set(ModelAttributes.GeneratedApi, value)
    }
