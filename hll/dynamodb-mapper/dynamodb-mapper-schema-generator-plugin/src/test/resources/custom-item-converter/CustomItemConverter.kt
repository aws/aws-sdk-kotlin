/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package my.custom.item.converter

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.NumberValueConverters
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringValueConverter
import aws.smithy.kotlin.runtime.ExperimentalApi
import org.example.CustomUser

@OptIn(ExperimentalApi::class)
public object MyCustomUserConverter : ItemConverter<CustomUser> by SimpleItemConverter(
    builderFactory = { CustomUser() },
    build = { this },
    descriptors = arrayOf(
        AttributeDescriptor(
            "id",
            CustomUser::id,
            CustomUser::id::set,
            NumberValueConverters.Int,
        ),
        AttributeDescriptor(
            "myCustomFirstName",
            CustomUser::givenName,
            CustomUser::givenName::set,
            StringValueConverter,
        ),
        AttributeDescriptor(
            "myCustomLastName",
            CustomUser::surname,
            CustomUser::surname::set,
            StringValueConverter,
        ),
        AttributeDescriptor(
            "myCustomAge",
            CustomUser::age,
            CustomUser::age::set,
            NumberValueConverters.Int,
        ),
    ),
)
