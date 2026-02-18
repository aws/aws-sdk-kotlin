/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeyType2Impl

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: ByteArray): KeyType.Key2<K1, ByteArray> = KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param N The type of [Number] used for the second attribute (e.g., [Int])
 * @param value2 The value of the second attribute
 */
public operator fun <K1, N : Number> KeyType.Key1<K1>.invoke(value2: N): KeyType.Key2<K1, N> = KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: String): KeyType.Key2<K1, String> = KeyType2Impl(value1, value2)
