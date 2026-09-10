/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.telemetry.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit

/**
 * Maps a telemetry unit string to a CloudWatch [StandardUnit].
 *
 * Targets the `Unit` member of `MetricDatum`; the authoritative list of accepted values is on
 * [MetricDatum.Unit](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Unit).
 * Every branch below has been checked against that list — an unrecognised enum name would be a
 * request-time rejection, not a compile error, since the value is a string on the wire.
 *
 * ### Why a mapping is needed at all
 *
 * The telemetry API's `units` is a free-form string, by convention following the UCUM codes that
 * OpenTelemetry uses (`"ms"`, `"By"`, `"1"`). CloudWatch instead takes a closed enum. Neither side
 * can be changed, so a translation table is unavoidable.
 *
 * Getting the unit right matters more than it appears: CloudWatch uses it for axis labels and for
 * unit-aware maths, and a duration published as `Count` renders as a bare number that a reader will
 * likely misinterpret as a request count.
 *
 * ### Failure behaviour
 *
 * An unrecognised unit maps to [StandardUnit.None] rather than throwing or dropping the datum. An
 * unknown unit is a cosmetic problem — the value is still correct and still charted — whereas
 * dropping the metric would lose data, and throwing would surface a telemetry detail inside a
 * user's request. Case is normalised because the conventions are inconsistent in practice (`"By"`
 * in UCUM, `"bytes"` in hand-written instrumentation).
 */
internal fun cloudWatchUnit(units: String?): StandardUnit = when (units?.lowercase()) {
    // Null and "1" (UCUM dimensionless) mean an unheaded quantity. Count is a better rendering
    // than None here: these are overwhelmingly counters, and Count labels the axis correctly.
    null, "", "1", "{count}" -> StandardUnit.Count

    // Duration units. "us" is the ASCII spelling of microseconds that UCUM uses in place of "μs".
    "s", "sec", "second", "seconds" -> StandardUnit.Seconds
    "ms", "millisecond", "milliseconds" -> StandardUnit.Milliseconds
    "us", "microsecond", "microseconds" -> StandardUnit.Microseconds

    // Size units. "by" is UCUM for byte; the SDK's recordPayloadSize extension emits it.
    "by", "byte", "bytes" -> StandardUnit.Bytes
    "kby", "kilobytes" -> StandardUnit.Kilobytes
    "mby", "megabytes" -> StandardUnit.Megabytes
    "bit", "bits" -> StandardUnit.Bits

    "%", "percent" -> StandardUnit.Percent

    // Rate units. Worth mapping because CloudWatch treats a rate differently from its base unit
    // when aggregating across periods.
    "by/s", "bytes/second" -> StandardUnit.BytesSecond
    "1/s", "count/second" -> StandardUnit.CountSecond

    else -> StandardUnit.None
}
