/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.BooleanExpr
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.FilterDsl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.UpdateDsl
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeyType
import aws.sdk.kotlin.services.dynamodb.model.ReturnValuesOnConditionCheckFailure

public sealed interface TransactWriteItemsRequestTableDsl<T> {
    public var puts: List<TransactWriteItemsAction.Put<T>>
    public fun put(item: T, block: Put.() -> Unit = { })

    public interface PartitionKey<T, PK : KeyType> : TransactWriteItemsRequestTableDsl<T> {
        public var conditionChecks: List<TransactWriteItemsAction.ConditionCheck<PK>>
        public fun conditionCheck(key: PK, block: ConditionCheck.() -> Unit = { })

        public var deletes: List<TransactWriteItemsAction.Delete<PK>>
        public fun delete(key: PK, block: Delete.() -> Unit = { })

        public var updates: List<TransactWriteItemsAction.Update<PK>>
        public fun update(key: PK, block: Update.() -> Unit = { })
    }

    public interface CompositeKey<T, PK : KeyType, SK : KeyType> : TransactWriteItemsRequestTableDsl<T> {
        public var conditionChecks: List<TransactWriteItemsAction.ConditionCheck<Pair<PK, SK>>>
        public fun conditionCheck(partitionKey: PK, sortKey: SK, block: ConditionCheck.() -> Unit = { })

        public var deletes: List<TransactWriteItemsAction.Delete<Pair<PK, SK>>>
        public fun delete(partitionKey: PK, sortKey: SK, block: Delete.() -> Unit = { })

        public var updates: List<TransactWriteItemsAction.Update<Pair<PK, SK>>>
        public fun update(partitionKey: PK, sortKey: SK, block: Update.() -> Unit = { })
    }

    public interface ConditionCheck {
        public fun condition(block: FilterDsl.() -> BooleanExpr)
        public var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
    }

    public interface Delete {
        public fun condition(block: FilterDsl.() -> BooleanExpr)
        public var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
    }

    public interface Put {
        public fun condition(block: FilterDsl.() -> BooleanExpr)
        public var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
    }

    public interface Update {
        public fun condition(block: FilterDsl.() -> BooleanExpr)
        public var returnValuesOnConditionCheckFailure: ReturnValuesOnConditionCheckFailure?
        public fun update(block: UpdateDsl.() -> Unit)
    }
}
