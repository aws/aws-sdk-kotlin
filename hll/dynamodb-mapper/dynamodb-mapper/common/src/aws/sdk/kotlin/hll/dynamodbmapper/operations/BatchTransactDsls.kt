/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.ParameterizingExpressionVisitor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.items.keysToItem
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItem
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItem
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

// DO NOT COMMIT THIS FILE!

internal sealed interface BatchWriteItemTable<T> {
    val putItems: List<T>
    val tableName: String

    interface PartitionKey<T, PK : KeyType> : BatchWriteItemTable<T> {
        val deleteKeys: List<PK>
        val schema: ItemSchema.PartitionKey<T, PK>
    }

    interface CompositeKey<T, PK : KeyType, SK : KeyType> : BatchWriteItemTable<T> {
        val deleteKeys: List<Pair<PK, SK>>
        val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

internal fun List<BatchWriteItemTable<*>>.convert(): Map<String, List<WriteRequest>> {
    fun <T, PK : KeyType> pkMap(table: BatchWriteItemTable.PartitionKey<T, PK>) =
        table.deleteKeys.map { key ->
            WriteRequest {
                deleteRequest {
                    this.key = keysToItem(table.schema, key)
                }
            }
        } + table.putItems.map { item ->
            WriteRequest {
                putRequest {
                    this.item = table.schema.converter.convertRight(item)
                }
            }
        }

    fun <T, PK : KeyType, SK : KeyType> ckMap(table: BatchWriteItemTable.CompositeKey<T, PK, SK>) =
        table.deleteKeys.map { (pk, sk) ->
            WriteRequest {
                deleteRequest {
                    this.key = keysToItem(table.schema, pk, sk)
                }
            }
        } + table.putItems.map { item ->
            WriteRequest {
                putRequest {
                    this.item = table.schema.converter.convertRight(item)
                }
            }
        }

    return associate { table ->
        val writes = when (table) {
            is BatchWriteItemTable.PartitionKey<*, *> -> pkMap(table)
            is BatchWriteItemTable.CompositeKey<*, *, *> -> ckMap(table)
        }

        table.tableName to writes
    }
}

internal sealed interface TransactGetItemsTable<T> {
    val tableName: String

    interface PartitionKey<T, PK : KeyType> : TransactGetItemsTable<T> {
        val keys: List<PK>
        val schema: ItemSchema.PartitionKey<T, PK>
    }

    interface CompositeKey<T, PK : KeyType, SK : KeyType> : TransactGetItemsTable<T> {
        val keys: List<Pair<PK, SK>>
        val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

internal fun List<TransactGetItemsTable<*>>.convert(): List<TransactGetItem> {
    fun <T, PK : KeyType> pkMap(table: TransactGetItemsTable.PartitionKey<T, PK>) =
        table.keys.map { key ->
            TransactGetItem {
                get {
                    this.key = keysToItem(table.schema, key)
                    tableName = table.tableName
                }
            }
        }

    fun <T, PK : KeyType, SK : KeyType> ckMap(table: TransactGetItemsTable.CompositeKey<T, PK, SK>) =
        table.keys.map { (pk, sk) ->
            TransactGetItem {
                get {
                    key = keysToItem(table.schema, pk, sk)
                    tableName = table.tableName
                }
            }
        }

    return flatMap { table ->
        when (table) {
            is TransactGetItemsTable.PartitionKey<*, *> -> pkMap(table)
            is TransactGetItemsTable.CompositeKey<*, *, *> -> ckMap(table)
        }
    }
}

internal sealed interface TransactWriteItemsTable<T> {
    val putItems: List<PutAction<T>>
    val tableName: String

    sealed interface ConditionCheckAction<T> {
        val condition: BooleanExpr
        val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?

        interface PartitionKey<T, PK : KeyType> : ConditionCheckAction<T> {
            val key: PK
        }

        interface CompositeKey<T, PK : KeyType, SK : KeyType> : ConditionCheckAction<T> {
            val partitionKey: PK
            val sortKey: SK
        }
    }

    sealed interface DeleteAction<T> {
        val condition: BooleanExpr?
        val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?

        interface PartitionKey<T, PK : KeyType> : DeleteAction<T> {
            val key: PK
        }

        interface CompositeKey<T, PK : KeyType, SK : KeyType> : DeleteAction<T> {
            val partitionKey: PK
            val sortKey: SK
        }
    }

    sealed interface PutAction<T> {
        val condition: BooleanExpr?
        val item: T
        val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
    }

    sealed interface UpdateAction<T> {
        val condition: BooleanExpr?
        val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
        val update: UpdateExpr

        interface PartitionKey<T, PK : KeyType> : UpdateAction<T> {
            val key: PK
        }

        interface CompositeKey<T, PK : KeyType, SK : KeyType> : UpdateAction<T> {
            val partitionKey: PK
            val sortKey: SK
        }
    }

    interface PartitionKey<T, PK : KeyType> : TransactWriteItemsTable<T> {
        val conditions: List<ConditionCheckAction.PartitionKey<T, PK>>
        val deletes: List<DeleteAction.PartitionKey<T, PK>>
        val updates: List<UpdateAction.PartitionKey<T, PK>>
        val schema: ItemSchema.PartitionKey<T, PK>
    }

    interface CompositeKey<T, PK : KeyType, SK : KeyType> : TransactWriteItemsTable<T> {
        val conditions: List<ConditionCheckAction.CompositeKey<T, PK, SK>>
        val deletes: List<DeleteAction.CompositeKey<T, PK, SK>>
        val updates: List<UpdateAction.CompositeKey<T, PK, SK>>
        val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

internal fun List<TransactWriteItemsTable<*>>.convert(expressionVisitorFactory: () -> ParameterizingExpressionVisitor): List<TransactWriteItem> {
    fun <T, PK : KeyType> pkMap(table: TransactWriteItemsTable.PartitionKey<T, PK>) =
        table.conditions.map { condition ->
            TransactWriteItem {
                conditionCheck {
                    val visitor = expressionVisitorFactory()
                    conditionExpression = condition.condition.accept(visitor)
                    expressionAttributeNames = visitor.expressionAttributeNames()
                    expressionAttributeValues = visitor.expressionAttributeValues()
                    key = keysToItem(table.schema, condition.key)
                    returnValuesOnConditionCheckFailure = condition.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        } + table.deletes.map { delete ->
            TransactWriteItem {
                delete {
                    delete.condition?.let { condition ->
                        val visitor = expressionVisitorFactory()
                        conditionExpression = condition.accept(visitor)
                        expressionAttributeNames = visitor.expressionAttributeNames()
                        expressionAttributeValues = visitor.expressionAttributeValues()
                    }
                    key = keysToItem(table.schema, delete.key)
                    returnValuesOnConditionCheckFailure = delete.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        } + table.putItems.map { put ->
            TransactWriteItem {
                put {
                    put.condition?.let { condition ->
                        val visitor = expressionVisitorFactory()
                        conditionExpression = condition.accept(visitor)
                        expressionAttributeNames = visitor.expressionAttributeNames()
                        expressionAttributeValues = visitor.expressionAttributeValues()
                    }
                    item = table.schema.converter.convertRight(put.item)
                    returnValuesOnConditionCheckFailure = put.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        } + table.updates.map { update ->
            TransactWriteItem {
                update {
                    update.condition?.let { condition ->
                        val visitor = expressionVisitorFactory()
                        conditionExpression = condition.accept(visitor)
                        expressionAttributeNames = visitor.expressionAttributeNames()
                        expressionAttributeValues = visitor.expressionAttributeValues()
                    }
                    key = keysToItem(table.schema, update.key)
                    returnValuesOnConditionCheckFailure = update.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        }

    fun <T, PK : KeyType, SK : KeyType> ckMap(table: TransactWriteItemsTable.CompositeKey<T, PK, SK>) =
        table.conditions.map { condition ->
            TransactWriteItem {
                conditionCheck {
                    val visitor = expressionVisitorFactory()
                    conditionExpression = condition.condition.accept(visitor)
                    expressionAttributeNames = visitor.expressionAttributeNames()
                    expressionAttributeValues = visitor.expressionAttributeValues()
                    key = keysToItem(table.schema, condition.partitionKey, condition.sortKey)
                    returnValuesOnConditionCheckFailure = condition.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        } + table.deletes.map { delete ->
            TransactWriteItem {
                delete {
                    delete.condition?.let { condition ->
                        val visitor = expressionVisitorFactory()
                        conditionExpression = condition.accept(visitor)
                        expressionAttributeNames = visitor.expressionAttributeNames()
                        expressionAttributeValues = visitor.expressionAttributeValues()
                    }
                    key = keysToItem(table.schema, delete.partitionKey, delete.sortKey)
                    returnValuesOnConditionCheckFailure = delete.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        } + table.putItems.map { put ->
            TransactWriteItem {
                put {
                    put.condition?.let { condition ->
                        val visitor = expressionVisitorFactory()
                        conditionExpression = condition.accept(visitor)
                        expressionAttributeNames = visitor.expressionAttributeNames()
                        expressionAttributeValues = visitor.expressionAttributeValues()
                    }
                    item = table.schema.converter.convertRight(put.item)
                    returnValuesOnConditionCheckFailure = put.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        } + table.updates.map { update ->
            TransactWriteItem {
                update {
                    update.condition?.let { condition ->
                        val visitor = expressionVisitorFactory()
                        conditionExpression = condition.accept(visitor)
                        expressionAttributeNames = visitor.expressionAttributeNames()
                        expressionAttributeValues = visitor.expressionAttributeValues()
                    }
                    key = keysToItem(table.schema, update.partitionKey, update.sortKey)
                    returnValuesOnConditionCheckFailure = update.returnValuesOnConditionCheckFailure
                    tableName = table.tableName
                }
            }
        }

    return flatMap { table ->
        when (table) {
            is TransactWriteItemsTable.PartitionKey<*, *> -> pkMap(table)
            is TransactWriteItemsTable.CompositeKey<*, *, *> -> ckMap(table)
        }
    }
}
