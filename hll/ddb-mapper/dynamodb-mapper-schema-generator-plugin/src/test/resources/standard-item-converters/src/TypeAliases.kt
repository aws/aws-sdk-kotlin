/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.example

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMappable
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey

typealias TaskArn = String
typealias TaskId = TaskArn
typealias AttemptCount = Int
typealias OptionalTaskArn = TaskArn?
typealias TaskList = List<TaskArn>
typealias TaskStatus = Status
typealias TaskDetails = Details

@DynamoDbMappable
data class Details(val description: String)

enum class Status {
    RUNNING,
    DONE,
}

@DynamoDbItem
data class TypeAliases(
    @DynamoDbPartitionKey val id: TaskArn,
    val taskId: TaskId,
    val attempts: AttemptCount,
    val optionalId: TaskArn?,
    val nullableAlias: OptionalTaskArn,
    val tasks: TaskList,
    val taskSet: Set<TaskArn>,
    val counts: Map<TaskArn, AttemptCount>,
    val status: TaskStatus,
    val statuses: Set<TaskStatus>,
    val statusCounts: Map<TaskStatus, AttemptCount>,
    val details: TaskDetails,
    val plain: String,
)
