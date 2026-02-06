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
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: Byte): KeyType.Key2<K1, Byte> =
    KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: ByteArray): KeyType.Key2<K1, ByteArray> =
    KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: Int): KeyType.Key2<K1, Int> =
    KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: Long): KeyType.Key2<K1, Long> =
    KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: Short): KeyType.Key2<K1, Short> =
    KeyType2Impl(value1, value2)

/**
 * Instantiates a new [KeyType] that builds from a single attribute value into two values. Additional values may be
 * added with one of the [invoke] extension function overloads.
 * @param value2 The value of the second attribute
 */
public operator fun <K1> KeyType.Key1<K1>.invoke(value2: String): KeyType.Key2<K1, String> =
    KeyType2Impl(value1, value2)
