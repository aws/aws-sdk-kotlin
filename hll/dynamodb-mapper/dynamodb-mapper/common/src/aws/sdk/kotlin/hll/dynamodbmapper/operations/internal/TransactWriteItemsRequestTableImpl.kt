/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateExpr
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsAction
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequestTable
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure

internal data class TransactWriteItemsActionConditionCheckImpl<E>(
    override val condition: BooleanExpr,
    override val entity: E,
    override val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
) : TransactWriteItemsAction.ConditionCheck<E>

internal data class TransactWriteItemsActionDeleteImpl<E>(
    override val condition: BooleanExpr?,
    override val entity: E,
    override val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
) : TransactWriteItemsAction.Delete<E>

internal data class TransactWriteItemsActionPutImpl<E>(
    override val condition: BooleanExpr?,
    override val entity: E,
    override val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
) : TransactWriteItemsAction.Put<E>

internal data class TransactWriteItemsActionUpdateImpl<E>(
    override val condition: BooleanExpr?,
    override val entity: E,
    override val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
    override val update: UpdateExpr,
) : TransactWriteItemsAction.Update<E>

internal data class TransactWriteItemsRequestTablePartitionKeyImpl<T, PK : KeyType>(
    override val conditionChecks: List<TransactWriteItemsAction.ConditionCheck<PK>>,
    override val deletes: List<TransactWriteItemsAction.Delete<PK>>,
    override val puts: List<TransactWriteItemsAction.Put<T>>,
    override val schema: ItemSchema.PartitionKey<T, PK>,
    override val tableName: String,
    override val updates: List<TransactWriteItemsAction.Update<PK>>,
) : TransactWriteItemsRequestTable.PartitionKey<T, PK>

internal data class TransactWriteItemsRequestTableCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    override val conditionChecks: List<TransactWriteItemsAction.ConditionCheck<Pair<PK, SK>>>,
    override val deletes: List<TransactWriteItemsAction.Delete<Pair<PK, SK>>>,
    override val puts: List<TransactWriteItemsAction.Put<T>>,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
    override val tableName: String,
    override val updates: List<TransactWriteItemsAction.Update<Pair<PK, SK>>>,
) : TransactWriteItemsRequestTable.CompositeKey<T, PK, SK>
