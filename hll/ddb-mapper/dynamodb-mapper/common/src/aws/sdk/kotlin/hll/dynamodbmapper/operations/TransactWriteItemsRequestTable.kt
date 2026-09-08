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
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.operations.internal.*
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItem

public sealed interface TransactWriteItemsAction<E> {
    public companion object { }

    public val condition: BooleanExpr?
    public val entity: E
    public val returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?

    public interface ConditionCheck<E> : TransactWriteItemsAction<E> {
        override val condition: BooleanExpr
    }

    public interface Delete<E> : TransactWriteItemsAction<E>

    public interface Put<E> : TransactWriteItemsAction<E>

    public interface Update<E> : TransactWriteItemsAction<E> {
        public val update: UpdateExpr
    }
}

public fun <E> TransactWriteItemsAction.Companion.ConditionCheck(
    condition: BooleanExpr,
    entity: E,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
): TransactWriteItemsAction.ConditionCheck<E> = TransactWriteItemsActionConditionCheckImpl(
    condition,
    entity,
    returnValuesOnConditionCheckFailure,
)

private fun TransactWriteItemsAction.ConditionCheck<*>.convert(
    tableName: String,
    key: Item,
) = TransactWriteItem {
    conditionCheck {
        this.tableName = tableName
        this.key = key
        returnValuesOnConditionCheckFailure = this@convert.returnValuesOnConditionCheckFailure

        val expressionVisitor = ParameterizingExpressionVisitor()
        conditionExpression = condition.accept(expressionVisitor)
        expressionAttributeNames = expressionVisitor.expressionAttributeNames()
        expressionAttributeValues = expressionVisitor.expressionAttributeValues()
    }
}

internal fun <T, PK : KeyType> TransactWriteItemsAction.ConditionCheck<PK>.convert(
    tableName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
) = convert(tableName, keysToItem(schema, entity))

internal fun <T, PK : KeyType, SK : KeyType> TransactWriteItemsAction.ConditionCheck<Pair<PK, SK>>.convert(
    tableName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
) = convert(tableName, keysToItem(schema, entity.first, entity.second))

public fun <E> TransactWriteItemsAction.Companion.Delete(
    condition: BooleanExpr?,
    entity: E,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
): TransactWriteItemsAction.Delete<E> = TransactWriteItemsActionDeleteImpl(
    condition,
    entity,
    returnValuesOnConditionCheckFailure,
)

private fun TransactWriteItemsAction.Delete<*>.convert(
    tableName: String,
    key: Item,
) = TransactWriteItem {
    delete {
        this.tableName = tableName
        this.key = key
        returnValuesOnConditionCheckFailure = this@convert.returnValuesOnConditionCheckFailure

        condition?.let { condition ->
            val expressionVisitor = ParameterizingExpressionVisitor()
            conditionExpression = condition.accept(expressionVisitor)
            expressionAttributeNames = expressionVisitor.expressionAttributeNames()
            expressionAttributeValues = expressionVisitor.expressionAttributeValues()
        }
    }
}

internal fun <T, PK : KeyType> TransactWriteItemsAction.Delete<PK>.convert(
    tableName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
) = convert(tableName, keysToItem(schema, entity))

internal fun <T, PK : KeyType, SK : KeyType> TransactWriteItemsAction.Delete<Pair<PK, SK>>.convert(
    tableName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
) = convert(tableName, keysToItem(schema, entity.first, entity.second))

public fun <E> TransactWriteItemsAction.Companion.Put(
    condition: BooleanExpr?,
    entity: E,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
): TransactWriteItemsAction.Put<E> = TransactWriteItemsActionPutImpl(
    condition,
    entity,
    returnValuesOnConditionCheckFailure,
)

private fun TransactWriteItemsAction.Put<*>.convert(
    tableName: String,
    item: Item,
) = TransactWriteItem {
    put {
        this.tableName = tableName
        this.item = item
        returnValuesOnConditionCheckFailure = this@convert.returnValuesOnConditionCheckFailure

        condition?.let { condition ->
            val expressionVisitor = ParameterizingExpressionVisitor()
            conditionExpression = condition.accept(expressionVisitor)
            expressionAttributeNames = expressionVisitor.expressionAttributeNames()
            expressionAttributeValues = expressionVisitor.expressionAttributeValues()
        }
    }
}

internal fun <T, PK : KeyType> TransactWriteItemsAction.Put<T>.convert(
    tableName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
) = convert(tableName, schema.converter.convertRight(entity))

internal fun <T, PK : KeyType, SK : KeyType> TransactWriteItemsAction.Put<T>.convert(
    tableName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
) = convert(tableName, schema.converter.convertRight(entity))

public fun <E> TransactWriteItemsAction.Companion.Update(
    condition: BooleanExpr?,
    entity: E,
    returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?,
    update: UpdateExpr,
): TransactWriteItemsAction.Update<E> = TransactWriteItemsActionUpdateImpl(
    condition,
    entity,
    returnValuesOnConditionCheckFailure,
    update,
)

private fun TransactWriteItemsAction.Update<*>.convert(
    tableName: String,
    key: Item,
) = TransactWriteItem {
    update {
        this.tableName = tableName
        this.key = key
        returnValuesOnConditionCheckFailure = this@convert.returnValuesOnConditionCheckFailure

        val expressionVisitor = ParameterizingExpressionVisitor()
        updateExpression = this@convert.update.accept(expressionVisitor)

        condition?.let { condition ->
            conditionExpression = condition.accept(expressionVisitor)
        }

        expressionAttributeNames = expressionVisitor.expressionAttributeNames()
        expressionAttributeValues = expressionVisitor.expressionAttributeValues()
    }
}

internal fun <T, PK : KeyType> TransactWriteItemsAction.Update<PK>.convert(
    tableName: String,
    schema: ItemSchema.PartitionKey<T, PK>,
) = convert(tableName, keysToItem(schema, entity))

internal fun <T, PK : KeyType, SK : KeyType> TransactWriteItemsAction.Update<Pair<PK, SK>>.convert(
    tableName: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
) = convert(tableName, keysToItem(schema, entity.first, entity.second))

public sealed interface TransactWriteItemsRequestTable<T> {
    public val puts: List<TransactWriteItemsAction.Put<T>>
    public val tableName: String

    public interface PartitionKey<T, PK : KeyType> : TransactWriteItemsRequestTable<T> {
        public val conditionChecks: List<TransactWriteItemsAction.ConditionCheck<PK>>
        public val deletes: List<TransactWriteItemsAction.Delete<PK>>
        public val updates: List<TransactWriteItemsAction.Update<PK>>
        public val schema: ItemSchema.PartitionKey<T, PK>
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : TransactWriteItemsRequestTable<T> {
        public val conditionChecks: List<TransactWriteItemsAction.ConditionCheck<Pair<PK, SK>>>
        public val deletes: List<TransactWriteItemsAction.Delete<Pair<PK, SK>>>
        public val updates: List<TransactWriteItemsAction.Update<Pair<PK, SK>>>
        public val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType> TransactWriteItemsRequestTable(
    conditionChecks: List<TransactWriteItemsAction.ConditionCheck<PK>>,
    deletes: List<TransactWriteItemsAction.Delete<PK>>,
    puts: List<TransactWriteItemsAction.Put<T>>,
    schema: ItemSchema.PartitionKey<T, PK>,
    tableName: String,
    updates: List<TransactWriteItemsAction.Update<PK>>,
): TransactWriteItemsRequestTable.PartitionKey<T, PK> = TransactWriteItemsRequestTablePartitionKeyImpl(
    conditionChecks,
    deletes,
    puts,
    schema,
    tableName,
    updates,
)

@Suppress("ktlint:standard:function-naming")
public fun <T, PK : KeyType, SK : KeyType> TransactWriteItemsRequestTable(
    conditionChecks: List<TransactWriteItemsAction.ConditionCheck<Pair<PK, SK>>>,
    deletes: List<TransactWriteItemsAction.Delete<Pair<PK, SK>>>,
    puts: List<TransactWriteItemsAction.Put<T>>,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
    tableName: String,
    updates: List<TransactWriteItemsAction.Update<Pair<PK, SK>>>,
): TransactWriteItemsRequestTable.CompositeKey<T, PK, SK> = TransactWriteItemsRequestTableCompositeKeyImpl(
    conditionChecks,
    deletes,
    puts,
    schema,
    tableName,
    updates,
)

internal fun List<TransactWriteItemsRequestTable<*>>.convert(): List<TransactWriteItem> {
    fun <T, PK : KeyType> pkMap(
        table: TransactWriteItemsRequestTable.PartitionKey<T, PK>,
    ) = table.conditionChecks.map { it.convert(table.tableName, table.schema) } +
        table.deletes.map { it.convert(table.tableName, table.schema) } +
        table.puts.map { it.convert(table.tableName, table.schema) } +
        table.updates.map { it.convert(table.tableName, table.schema) }

    fun <T, PK : KeyType, SK : KeyType> ckMap(
        table: TransactWriteItemsRequestTable.CompositeKey<T, PK, SK>,
    ) = table.conditionChecks.map { it.convert(table.tableName, table.schema) } +
        table.deletes.map { it.convert(table.tableName, table.schema) } +
        table.puts.map { it.convert(table.tableName, table.schema) } +
        table.updates.map { it.convert(table.tableName, table.schema) }

    return flatMap { table ->
        when (table) {
            is TransactWriteItemsRequestTable.PartitionKey<*, *> -> pkMap(table)
            is TransactWriteItemsRequestTable.CompositeKey<*, *, *> -> ckMap(table)
        }
    }
}
