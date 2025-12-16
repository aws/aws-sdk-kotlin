/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeyType4Impl

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(
    value4: ByteArray,
): KeyType.Key4<K1, K2, K3, ByteArray> = KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param N The type of [Number] used for the fourth attribute (e.g., [Int])
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3, N : Number> KeyType.Key3<K1, K2, K3>.invoke(
    value4: N,
): KeyType.Key4<K1, K2, K3, N> = KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(
    value4: String,
): KeyType.Key4<K1, K2, K3, String> = KeyType4Impl(value1, value2, value3, value4)
