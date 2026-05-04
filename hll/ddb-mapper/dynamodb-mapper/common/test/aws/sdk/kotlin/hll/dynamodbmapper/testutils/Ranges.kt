/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

// Weirdly, Kotlin stdlib doesn't have range implementations for UByte and UShort (but it _does_ have UInt and ULong).
// So we're rolling our own here!
data class UByteRange(override val start: UByte, override val endInclusive: UByte) : ClosedRange<UByte>
data class UShortRange(override val start: UShort, override val endInclusive: UShort) : ClosedRange<UShort>
