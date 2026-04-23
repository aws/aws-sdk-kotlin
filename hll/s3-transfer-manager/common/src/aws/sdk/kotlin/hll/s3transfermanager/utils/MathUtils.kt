/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.s3transfermanager.utils

/**
 * Returns the ceiling of the division
 *
 * This means the result is rounded up to the nearest integer if the dividend is not
 * evenly divisible by the divisor
 */
internal fun ceilDiv(dividend: Long, divisor: Long): Long {
    val div = dividend / divisor
    val remainder = dividend % divisor
    return if (remainder != 0L) {
        div + 1
    } else {
        div
    }
}
