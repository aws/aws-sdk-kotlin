/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.smithy.kotlin.runtime.content.buildDocument
import kotlin.test.Test

class DocumentValueConverterTest : ValueConvertersTest() {
    @Test
    fun testKitchenSink() = given(DocumentValueConverter.Default) {
        val expectedDocument = buildDocument {
            "name" to "Ian"
            "pets" to buildList {
                add("Arrayah")
                add("Coffee")
                add("Willow")
                add("Bambi")
                add("Ginny")
                add("Tori")
                add("Chewy")
                add("Maisy")
                add("Pineapple")
                add("Gizmo")
                add("Bug")
            }
            "inactive?" to false
            "employer" to buildDocument {
                "name" to "Amazon"
                "founded" to 1994
                "offices" to buildList {
                    add("Seattle")
                    add("New York")
                    add("Boston")
                }
                "parentCompany" to null
            }
            "favoriteThings" to buildList {
                add("chocolate chip cookies")
                add(13)
                add(true)
                add(null)
            }
        }

        val expectedValue = mapOf(
            "name" to "Ian",
            "pets" to listOf(
                "Arrayah",
                "Coffee",
                "Willow",
                "Bambi",
                "Ginny",
                "Tori",
                "Chewy",
                "Maisy",
                "Pineapple",
                "Gizmo",
                "Bug",
            ),
            "inactive?" to false,
            "employer" to mapOf(
                "name" to "Amazon",
                "founded" to 1994,
                "offices" to listOf(
                    "Seattle",
                    "New York",
                    "Boston",
                ),
                "parentCompany" to null,
            ),
            "favoriteThings" to listOf(
                "chocolate chip cookies",
                13,
                true,
                null,
            ),
        )

        expectedDocument inDdbIs expectedValue
    }
}
