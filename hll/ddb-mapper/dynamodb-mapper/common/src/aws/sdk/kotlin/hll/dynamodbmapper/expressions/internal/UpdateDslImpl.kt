/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*

internal class UpdateDslImpl(
    private val updates: MutableMap<AttributePath, UpdateClauseExpr> = mutableMapOf(),
) : UpdateDsl {
    constructor(updateExpr: UpdateExpr?) : this(updateExpr?.updates ?: listOf())
    constructor(updates: List<UpdateClauseExpr>) : this(updates.associateBy { it.target }.toMutableMap())

    private val set = UpdateSetImpl(updates)
    private val remove = UpdateRemoveImpl(updates)
    private val add = UpdateAddImpl(updates)
    private val delete = UpdateDeleteImpl(updates)

    override fun set(block: UpdateDsl.Set.() -> Unit) = set.block()
    override fun remove(block: UpdateDsl.Remove.() -> Unit) = remove.block()
    override fun add(block: UpdateDsl.Add.() -> Unit) = add.block()
    override fun delete(block: UpdateDsl.Delete.() -> Unit) = delete.block()

    fun toExpression(): UpdateExpr = UpdateExpr(updates.values.toList())
}

internal abstract class UpdateClauseImpl(
    val action: UpdateAction,
    private val updates: MutableMap<AttributePath, UpdateClauseExpr>,
) {
    fun AttributePath.update(value: Expression) {
        val clauseExp = UpdateClauseExpr(action, this, value)
        val previousUpdate = updates.put(clauseExp.target, clauseExp)
        if (previousUpdate != null) {
            println("Warning: Replacing previous update of ${previousUpdate.target}") // FIXME log a proper warning
        }
    }
}

internal class UpdateSetImpl(updates: MutableMap<AttributePath, UpdateClauseExpr>) :
    UpdateClauseImpl(UpdateAction.SET, updates),
    UpdateDsl.Set {

    override val attr = AttrImpl

    override fun Attr.set(key: String, value: Expression) = get(key).update(value)
    override fun AttributePath.set(index: Int, value: Expression) = get(index).update(value)
    override fun AttributePath.set(key: String, value: Expression) = get(key).update(value)

    override fun AttributePath.appending(
        value: Expression,
    ): Expression = ScalarFuncExpr(ScalarFunc.LIST_APPEND, this, value)

    override fun List<Any?>.appending(
        value: Expression,
    ): Expression = ScalarFuncExpr(ScalarFunc.LIST_APPEND, LiteralExpr(this), value)

    override fun AttributePath.orElse(
        value: Expression,
    ): Expression = ScalarFuncExpr(ScalarFunc.IF_NOT_EXISTS, this, value)

    override fun Expression.plus(
        value: Expression,
    ): Expression = AdditiveExpr(AdditiveOperation.ADD, this, value)

    override fun Expression.minus(
        value: Expression,
    ): Expression = AdditiveExpr(AdditiveOperation.SUBTRACT, this, value)
}

internal class UpdateRemoveImpl(updates: MutableMap<AttributePath, UpdateClauseExpr>) :
    UpdateClauseImpl(UpdateAction.REMOVE, updates),
    UpdateDsl.Remove {

    override val attr = AttrImpl

    override fun AttributePath.unaryMinus() = update(LiteralExpr(null))
}

internal class UpdateAddImpl(updates: MutableMap<AttributePath, UpdateClauseExpr>) :
    UpdateClauseImpl(UpdateAction.ADD, updates),
    UpdateDsl.Add {

    override val attr: Attr = AttrImpl

    override fun AttributePath.plusAssign(value: Expression) = update(value)
}

internal class UpdateDeleteImpl(updates: MutableMap<AttributePath, UpdateClauseExpr>) :
    UpdateClauseImpl(UpdateAction.DELETE, updates),
    UpdateDsl.Delete {

    override val attr: Attr = AttrImpl

    override fun AttributePath.minusAssign(value: Expression) = update(value)
}
