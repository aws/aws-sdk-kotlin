/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.dynamodb

/**
 * Checks if a Dynamo DB table exists based on its name
 */
internal suspend fun DynamoDbClient.tableExists(name: String): Boolean = this.listTables {}.tableNames?.contains(name) ?: false
