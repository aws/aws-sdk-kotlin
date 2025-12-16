/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example

import a.different.pkg.HealthcareConverter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttributeConverter
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.MonoConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

@DynamoDbItem
data class Employee(
    @DynamoDbPartitionKey
    var id: Int = 1,
    var givenName: String = "Johnny",
    var surname: String = "Appleseed",

    @DynamoDbAttributeConverter(OccupationConverter::class)
    var occupation: Occupation = Occupation("Student", 0),

    @DynamoDbAttributeConverter(HealthcareConverter::class)
    var healthcare: Healthcare = Healthcare(false),
)

data class Occupation(val title: String, val salary: Int)
data class Healthcare(val enrolled: Boolean)

class OccupationConverter : ValueConverter<Occupation> {
    override val right = MonoConverter<Occupation, AttributeValue> { AttributeValue.S(it.title + "#" + it.salary) }

    override val left = MonoConverter<AttributeValue, Occupation> {
        val content = it.asS()
        val (title, salary) = content.split("#")
        Occupation(title, salary.toInt())
    }
}
