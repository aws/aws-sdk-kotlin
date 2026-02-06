/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Verifies that all elements in this collection are distinct. If duplicates exist, an [IllegalArgumentException] is
 * thrown that details precisely which elements are duplicates.
 */
@InternalSdkApi
public fun <T, C : Iterable<T>> C.requireAllDistinct(): C {
    val collection = this
    val itemCounts = buildMap {
        collection.forEach { element ->
            compute(element) { _, existingCount -> (existingCount ?: 0) + 1 }
        }
    }

    val duplicates = itemCounts.filter { (_, count) -> count > 1 }.keys
    require(duplicates.isEmpty()) { "Found duplicated items $duplicates in collection $this" }

    return collection
}
