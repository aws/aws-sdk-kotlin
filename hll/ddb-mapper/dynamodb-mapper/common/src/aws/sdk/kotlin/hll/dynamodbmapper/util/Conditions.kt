/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
internal fun <T : Any> requireNull(value: T?, lazyMessage: () -> Any): T? {
    contract {
        returns() implies (value == null)
    }

    if (value == null) {
        return null
    } else {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}
