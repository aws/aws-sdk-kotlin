/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeySpec4Impl

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenByte(name: String): KeySpec.Key4<K1, K2, K3, Byte> = KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.byte(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenByteArray(name: String): KeySpec.Key4<K1, K2, K3, ByteArray> = KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.byteArray(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenInt(name: String): KeySpec.Key4<K1, K2, K3, Int> = KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.int(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenLong(name: String): KeySpec.Key4<K1, K2, K3, Long> = KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.long(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenShort(name: String): KeySpec.Key4<K1, K2, K3, Short> = KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.short(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenString(name: String): KeySpec.Key4<K1, K2, K3, String> = KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.string(name))
