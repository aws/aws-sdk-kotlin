/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.internal.KeySpec2Impl

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenByteArray], [thenNumber], and [thenString].
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenByteArray(name: String): KeySpec.Key2<K1, ByteArray> =
    KeySpec2Impl(attr1, KeyAttrSpec.byteArray(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenByteArray], [thenNumber], and [thenString].
 * @param N The type of [Number] used for the second attribute (e.g., [Int])
 * @param name The name of the second attribute
 */
public fun <K1, N : Number> KeySpec.Key1<K1>.thenNumber(name: String): KeySpec.Key2<K1, N> =
    KeySpec2Impl(attr1, KeyAttrSpec.number(name))

/**
 * Instantiates a new [KeySpec] that builds from a single attribute into two attributes. Additional attributes may be
 * added with the extension methods [thenByteArray], [thenNumber], and [thenString].
 * @param name The name of the second attribute
 */
public fun <K1> KeySpec.Key1<K1>.thenString(name: String): KeySpec.Key2<K1, String> =
    KeySpec2Impl(attr1, KeyAttrSpec.string(name))
