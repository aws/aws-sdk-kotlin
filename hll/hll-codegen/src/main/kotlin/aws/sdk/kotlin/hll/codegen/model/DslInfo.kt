/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Contains information about types relevant to generating DSL methods
 * @param interfaceType The interface type used as the receiver for a DSL block. This should generally be a `public`
 * type.
 * @param implType The implementation type used to actually invoke the DSL block. This should generally be an `internal`
 * type.
 * @param implSingleton A flag indicating whether [implType] is a "singleton type" that can be referenced without
 * instantiation
 */
@InternalSdkApi
public data class DslInfo(
    val interfaceType: TypeRef,
    val implType: TypeRef,
    val implSingleton: Boolean = false,
    val implFinalizer: String? = null,
)

@InternalSdkApi
public val Member.dslInfo: DslInfo?
    get() = attributes.getOrNull(ModelAttributes.DslInfo)
