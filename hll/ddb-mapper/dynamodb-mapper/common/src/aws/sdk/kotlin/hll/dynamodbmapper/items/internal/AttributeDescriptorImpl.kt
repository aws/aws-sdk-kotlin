/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter

internal data class AttributeDescriptorImpl<A, T, B>(
    override val name: String,
    override val getter: (T) -> A,
    override val setter: B.(A) -> Unit,
    override val converter: ValueConverter<A>,
) : AttributeDescriptor<A, T, B>
