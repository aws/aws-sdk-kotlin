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
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(value4: Byte): KeyType.Key4<K1, K2, K3, Byte> =
    KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(
    value4: ByteArray,
): KeyType.Key4<K1, K2, K3, ByteArray> = KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(value4: Int): KeyType.Key4<K1, K2, K3, Int> =
    KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(value4: Long): KeyType.Key4<K1, K2, K3, Long> =
    KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(value4: Short): KeyType.Key4<K1, K2, K3, Short> =
    KeyType4Impl(value1, value2, value3, value4)

/**
 * Instantiates a new [KeyType] that builds from three attribute values into four values
 * @param value4 The value of the fourth attribute
 */
public operator fun <K1, K2, K3> KeyType.Key3<K1, K2, K3>.invoke(value4: String): KeyType.Key4<K1, K2, K3, String> =
    KeyType4Impl(value1, value2, value3, value4)
