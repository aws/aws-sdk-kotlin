/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.telemetry.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit

/**
 * Maps a telemetry unit string, by convention a
 * [UCUM code](https://ucum.org/ucum), to a CloudWatch [StandardUnit]. Unrecognised units map to
 * [StandardUnit.None]; the value is still published.
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html#ACW-Type-MetricDatum-Unit">MetricDatum.Unit</a>
 */
internal fun cloudWatchUnit(units: String?): StandardUnit = when (units?.lowercase()) {
    null, "", "1", "{count}" -> StandardUnit.Count

    "s", "sec", "second", "seconds" -> StandardUnit.Seconds
    "ms", "millisecond", "milliseconds" -> StandardUnit.Milliseconds
    "us", "microsecond", "microseconds" -> StandardUnit.Microseconds

    "by", "byte", "bytes" -> StandardUnit.Bytes
    "kby", "kilobytes" -> StandardUnit.Kilobytes
    "mby", "megabytes" -> StandardUnit.Megabytes
    "bit", "bits" -> StandardUnit.Bits

    "%", "percent" -> StandardUnit.Percent

    "by/s", "bytes/second" -> StandardUnit.BytesSecond
    "1/s", "count/second" -> StandardUnit.CountSecond

    else -> StandardUnit.None
}
