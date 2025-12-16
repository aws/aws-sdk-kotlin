/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeyType3Impl

/**
 * Instantiates a new [KeyType] that builds from two attribute values into three values. Additional values may be added
 * with one of the [invoke] extension function overloads.
 * @param value3 The value of the third attribute
 */
public operator fun <K1, K2> KeyType.Key2<K1, K2>.invoke(value3: ByteArray): KeyType.Key3<K1, K2, ByteArray> =
    KeyType3Impl(value1, value2, value3)

/**
 * Instantiates a new [KeyType] that builds from two attribute values into three values. Additional values may be added
 * with one of the [invoke] extension function overloads.
 * @param N The type of [Number] used for the third attribute (e.g., [Int])
 * @param value3 The value of the third attribute
 */
public operator fun <K1, K2, N : Number> KeyType.Key2<K1, K2>.invoke(value3: N): KeyType.Key3<K1, K2, N> =
    KeyType3Impl(value1, value2, value3)

/**
 * Instantiates a new [KeyType] that builds from two attribute values into three values. Additional values may be added
 * with one of the [invoke] extension function overloads.
 * @param value3 The value of the third attribute
 */
public operator fun <K1, K2> KeyType.Key2<K1, K2>.invoke(value3: String): KeyType.Key3<K1, K2, String> =
    KeyType3Impl(value1, value2, value3)
