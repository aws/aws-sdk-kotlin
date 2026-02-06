/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeySpec3Impl

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenByte(name: String): KeySpec.Key3<K1, K2, Byte> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.byte(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenByteArray(name: String): KeySpec.Key3<K1, K2, ByteArray> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.byteArray(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenInt(name: String): KeySpec.Key3<K1, K2, Int> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.int(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenLong(name: String): KeySpec.Key3<K1, K2, Long> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.long(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenShort(name: String): KeySpec.Key3<K1, K2, Short> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.short(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenString(name: String): KeySpec.Key3<K1, K2, String> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.string(name))
