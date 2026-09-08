/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType

internal data class KeyType1Impl<K1>(override val value1: K1) : KeyType.Key1<K1>

internal data class KeyType2Impl<K1, K2>(override val value1: K1, override val value2: K2) : KeyType.Key2<K1, K2>

internal data class KeyType3Impl<K1, K2, K3>(
    override val value1: K1,
    override val value2: K2,
    override val value3: K3,
) : KeyType.Key3<K1, K2, K3>

internal data class KeyType4Impl<K1, K2, K3, K4>(
    override val value1: K1,
    override val value2: K2,
    override val value3: K3,
    override val value4: K4,
) : KeyType.Key4<K1, K2, K3, K4>

@Suppress("FunctionName")
internal fun <K1> Key(value1: K1): KeyType.Key1<K1> = KeyType1Impl(value1)

internal val KeyType.values: List<Any?>
    get() = when (this) {
        is KeyType.Key1<*> -> listOf(value1)
        is KeyType.Key2<*, *> -> listOf(value1, value2)
        is KeyType.Key3<*, *, *> -> listOf(value1, value2, value3)
        is KeyType.Key4<*, *, *, *> -> listOf(value1, value2, value3, value4)
    }
