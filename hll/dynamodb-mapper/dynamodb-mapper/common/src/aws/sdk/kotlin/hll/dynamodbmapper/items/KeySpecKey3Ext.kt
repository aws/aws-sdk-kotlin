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
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenByteArray(name: String): KeySpec.Key4<K1, K2, K3, ByteArray> =
    KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.byteArray(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param N The type of [Number] used for the fourth attribute (e.g., [Int])
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3, N : Number> KeySpec.Key3<K1, K2, K3>.thenNumber(name: String): KeySpec.Key4<K1, K2, K3, N> =
    KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.number(name))

/**
 * Instantiates a new [KeySpec] that builds from three attributes into four attributes
 * @param name The name of the fourth attribute
 */
public fun <K1, K2, K3> KeySpec.Key3<K1, K2, K3>.thenString(name: String): KeySpec.Key4<K1, K2, K3, String> =
    KeySpec4Impl(attr1, attr2, attr3, KeyAttrSpec.string(name))
