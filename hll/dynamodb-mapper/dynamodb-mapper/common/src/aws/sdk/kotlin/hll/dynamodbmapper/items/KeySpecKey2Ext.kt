/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeySpec3Impl

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenByteArray], [thenNumber], and [thenString].
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenByteArray(name: String): KeySpec.Key3<K1, K2, ByteArray> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.byteArray(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenByteArray], [thenNumber], and [thenString].
 * @param N The type of [Number] used for the third attribute (e.g., [Int])
 * @param name The name of the third attribute
 */
public fun <K1, K2, N : Number> KeySpec.Key2<K1, K2>.thenNumber(name: String): KeySpec.Key3<K1, K2, N> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.number(name))

/**
 * Instantiates a new [KeySpec] that builds from two attributes into three attributes. Additional attributes may be
 * added with the extension methods [thenByteArray], [thenNumber], and [thenString].
 * @param name The name of the third attribute
 */
public fun <K1, K2> KeySpec.Key2<K1, K2>.thenString(name: String): KeySpec.Key3<K1, K2, String> =
    KeySpec3Impl(attr1, attr2, KeyAttrSpec.string(name))
