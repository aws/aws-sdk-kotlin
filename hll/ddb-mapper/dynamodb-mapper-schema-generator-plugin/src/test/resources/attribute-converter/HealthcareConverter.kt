/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package a.different.pkg

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.Healthcare

class HealthcareConverter : ValueConverter<Healthcare> {
    override fun convertRight(from: Healthcare): AttributeValue = AttributeValue.S(from.enrolled.toString())

    override fun convertLeft(from: AttributeValue): Healthcare {
        val content = from.asS()
        val enrolled = (content == "true")
        return Healthcare(enrolled)
    }
}
