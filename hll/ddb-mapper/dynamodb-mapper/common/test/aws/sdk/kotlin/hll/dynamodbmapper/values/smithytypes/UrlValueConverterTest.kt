/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.smithy.kotlin.runtime.net.url.Url
import kotlin.test.Test

private const val URL = "https://foo.bar.baz/qux?quux=corge#grault"

class UrlValueConverterTest : ValueConvertersTest() {
    @Test
    fun testUrlConverter() = given(UrlValueConverter) {
        Url.parse(URL) inDdbIs URL
    }
}
