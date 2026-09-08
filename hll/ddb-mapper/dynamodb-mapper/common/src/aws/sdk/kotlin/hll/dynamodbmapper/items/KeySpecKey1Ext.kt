/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeySpec2Impl

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenByte(name: String): KeySpec.Key2<K1, Byte> = KeySpec2Impl(attr1, KeyAttrSpec.byte(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenByteArray(name: String): KeySpec.Key2<K1, ByteArray> = KeySpec2Impl(attr1, KeyAttrSpec.byteArray(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenInt(name: String): KeySpec.Key2<K1, Int> = KeySpec2Impl(attr1, KeyAttrSpec.int(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenLong(name: String): KeySpec.Key2<K1, Long> = KeySpec2Impl(attr1, KeyAttrSpec.long(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenShort(name: String): KeySpec.Key2<K1, Short> = KeySpec2Impl(attr1, KeyAttrSpec.short(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenInt], [thenLong], [thenString], etc.
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenString(name: String): KeySpec.Key2<K1, String> = KeySpec2Impl(attr1, KeyAttrSpec.string(name))
