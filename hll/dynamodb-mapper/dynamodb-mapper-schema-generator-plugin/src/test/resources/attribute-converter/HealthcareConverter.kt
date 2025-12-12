/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package a.different.pkg

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.example.Healthcare

class HealthcareConverter : ValueConverter<Healthcare> {
    override val right = MonoConverter<Healthcare, AttributeValue> { AttributeValue.S(it.enrolled.toString()) }

    override val left = MonoConverter<AttributeValue, Healthcare> {
        val content = it.asS()
        val enrolled = (content == "true")
        Healthcare(enrolled)
    }
}
