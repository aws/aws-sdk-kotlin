/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.FilterDsl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateDsl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.FilterDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.UpdateDslImpl
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsAction
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequestTable
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TransactWriteItemsRequestTableDsl
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure

internal class TransactWriteItemsRequestTableDslConditionCheckImpl : TransactWriteItemsRequestTableDsl.ConditionCheck {
    var conditionCheckExpression: BooleanExpr? = null

    override fun condition(block: FilterDsl.() -> BooleanExpr) {
        conditionCheckExpression = FilterDslImpl.run(block)
    }

    override var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null
}

internal class TransactWriteItemsRequestTableDslDeleteImpl : TransactWriteItemsRequestTableDsl.Delete {
    var conditionCheckExpression: BooleanExpr? = null

    override fun condition(block: FilterDsl.() -> BooleanExpr) {
        conditionCheckExpression = FilterDslImpl.run(block)
    }

    override var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null
}

internal class TransactWriteItemsRequestTableDslPutImpl : TransactWriteItemsRequestTableDsl.Put {
    var conditionCheckExpression: BooleanExpr? = null

    override fun condition(block: FilterDsl.() -> BooleanExpr) {
        conditionCheckExpression = FilterDslImpl.run(block)
    }

    override var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null
}

internal class TransactWriteItemsRequestTableDslUpdateImpl : TransactWriteItemsRequestTableDsl.Update {
    var conditionCheckExpression: BooleanExpr? = null
    var updateExpression: UpdateExpr? = null

    override fun condition(block: FilterDsl.() -> BooleanExpr) {
        conditionCheckExpression = FilterDslImpl.run(block)
    }

    override var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure? = null

    override fun update(block: UpdateDsl.() -> Unit) {
        updateExpression = UpdateDslImpl().apply(block).toExpression()
    }
}

internal class TransactWriteItemsRequestTableDslPartitionKeyImpl<T, PK : KeyType>(
    val existingTables: List<TransactWriteItemsRequestTable<*>>?,
    val table: Table.PartitionKey<T, PK>,
) : TransactWriteItemsRequestTableDsl.PartitionKey<T, PK> {
    override var conditionChecks: List<TransactWriteItemsAction.ConditionCheck<PK>> = mutableListOf()
    override var deletes: List<TransactWriteItemsAction.Delete<PK>> = mutableListOf()
    override var puts: List<TransactWriteItemsAction.Put<T>> = mutableListOf()
    override var updates: List<TransactWriteItemsAction.Update<PK>> = mutableListOf()

    override fun conditionCheck(key: PK, block: TransactWriteItemsRequestTableDsl.ConditionCheck.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslConditionCheckImpl()
        dsl.block()
        conditionChecks += TransactWriteItemsActionConditionCheckImpl(
            requireNotNull(dsl.conditionCheckExpression) {
                "A `condition` block must be specified for `conditionCheck` actions"
            },
            key,
            dsl.returnValuesOnConditionCheckFailure,
        )
    }

    override fun delete(key: PK, block: TransactWriteItemsRequestTableDsl.Delete.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslDeleteImpl()
        dsl.block()
        deletes += TransactWriteItemsActionDeleteImpl(
            dsl.conditionCheckExpression,
            key,
            dsl.returnValuesOnConditionCheckFailure,
        )
    }

    override fun put(item: T, block: TransactWriteItemsRequestTableDsl.Put.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslPutImpl()
        dsl.block()
        puts += TransactWriteItemsActionPutImpl(
            dsl.conditionCheckExpression,
            item,
            dsl.returnValuesOnConditionCheckFailure,
        )
    }

    override fun update(key: PK, block: TransactWriteItemsRequestTableDsl.Update.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslUpdateImpl()
        dsl.block()
        updates += TransactWriteItemsActionUpdateImpl(
            dsl.conditionCheckExpression,
            key,
            dsl.returnValuesOnConditionCheckFailure,
            requireNotNull(dsl.updateExpression) {
                "An `update` block must be specified for `update` actions"
            },
        )
    }

    private fun toTable() = TransactWriteItemsRequestTable(
        conditionChecks,
        deletes,
        puts,
        table.schema,
        table.tableName,
        updates,
    )

    internal fun toTables(): List<TransactWriteItemsRequestTable<*>> = existingTables.orEmpty() + toTable()
}

internal class TransactWriteItemsRequestTableDslCompositeKeyImpl<T, PK : KeyType, SK : KeyType>(
    val existingTables: List<TransactWriteItemsRequestTable<*>>?,
    val table: Table.CompositeKey<T, PK, SK>,
) : TransactWriteItemsRequestTableDsl.CompositeKey<T, PK, SK> {
    override var conditionChecks: List<TransactWriteItemsAction.ConditionCheck<Pair<PK, SK>>> = mutableListOf()
    override var deletes: List<TransactWriteItemsAction.Delete<Pair<PK, SK>>> = mutableListOf()
    override var puts: List<TransactWriteItemsAction.Put<T>> = mutableListOf()
    override var updates: List<TransactWriteItemsAction.Update<Pair<PK, SK>>> = mutableListOf()

    override fun conditionCheck(
        partitionKey: PK,
        sortKey: SK,
        block: TransactWriteItemsRequestTableDsl.ConditionCheck.() -> Unit,
    ) {
        val dsl = TransactWriteItemsRequestTableDslConditionCheckImpl()
        dsl.block()
        conditionChecks += TransactWriteItemsActionConditionCheckImpl(
            requireNotNull(dsl.conditionCheckExpression) {
                "A `condition` block must be specified for `conditionCheck` actions"
            },
            partitionKey to sortKey,
            dsl.returnValuesOnConditionCheckFailure,
        )
    }

    override fun delete(partitionKey: PK, sortKey: SK, block: TransactWriteItemsRequestTableDsl.Delete.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslDeleteImpl()
        dsl.block()
        deletes += TransactWriteItemsActionDeleteImpl(
            dsl.conditionCheckExpression,
            partitionKey to sortKey,
            dsl.returnValuesOnConditionCheckFailure,
        )
    }

    override fun put(item: T, block: TransactWriteItemsRequestTableDsl.Put.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslPutImpl()
        dsl.block()
        puts += TransactWriteItemsActionPutImpl(
            dsl.conditionCheckExpression,
            item,
            dsl.returnValuesOnConditionCheckFailure,
        )
    }

    override fun update(partitionKey: PK, sortKey: SK, block: TransactWriteItemsRequestTableDsl.Update.() -> Unit) {
        val dsl = TransactWriteItemsRequestTableDslUpdateImpl()
        dsl.block()
        updates += TransactWriteItemsActionUpdateImpl(
            dsl.conditionCheckExpression,
            partitionKey to sortKey,
            dsl.returnValuesOnConditionCheckFailure,
            requireNotNull(dsl.updateExpression) {
                "An `update` block must be specified for `update` actions"
            },
        )
    }

    private fun toTable() = TransactWriteItemsRequestTable(
        conditionChecks,
        deletes,
        puts,
        table.schema,
        table.tableName,
        updates,
    )

    internal fun toTables(): List<TransactWriteItemsRequestTable<*>> = existingTables.orEmpty() + toTable()
}
