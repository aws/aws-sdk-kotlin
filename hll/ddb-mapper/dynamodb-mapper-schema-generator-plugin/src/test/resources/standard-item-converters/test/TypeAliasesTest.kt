/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import org.example.dynamodbmapper.generatedschemas.TypeAliasesConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeAliasesTest {
    @Test
    fun converterTest() {
        val item = TypeAliases(
            id = "arn:task",
            taskId = "task-id",
            attempts = 3,
            optionalId = "optional",
            nullableAlias = "nullable",
            tasks = listOf("first", "second"),
            taskSet = setOf("first", "second"),
            counts = mapOf("first" to 1, "second" to 2),
            status = Status.RUNNING,
            statuses = setOf(Status.RUNNING, Status.DONE),
            statusCounts = mapOf(Status.RUNNING to 1, Status.DONE to 2),
            details = Details("details"),
            plain = "plain",
        )
        for (value in listOf(item, item.copy(optionalId = null, nullableAlias = null))) {
            val converted = TypeAliasesConverter.convertRight(value)
            assertEquals(value, TypeAliasesConverter.convertLeft(converted))
        }
    }
}
