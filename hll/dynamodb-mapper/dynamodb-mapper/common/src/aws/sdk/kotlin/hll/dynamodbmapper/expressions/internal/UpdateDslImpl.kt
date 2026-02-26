/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*

internal data class UpdateDslImpl(
    val set: UpdateSetImpl = UpdateSetImpl(),
    val remove: UpdateRemoveImpl = UpdateRemoveImpl(),
    val add: UpdateAddImpl = UpdateAddImpl(),
    val delete: UpdateDeleteImpl = UpdateDeleteImpl(),
) : UpdateDsl {
    override fun set(block: UpdateDsl.Set.() -> Unit) = set.block()
    override fun remove(block: UpdateDsl.Remove.() -> Unit) = remove.block()
    override fun add(block: UpdateDsl.Add.() -> Unit) = add.block()
    override fun delete(block: UpdateDsl.Delete.() -> Unit) = delete.block()

    fun toExpression(): UpdateExpr = UpdateExpr(
        UpdateExpr.Clause(set.updates),
        UpdateExpr.Clause(remove.updates),
        UpdateExpr.Clause(add.updates),
        UpdateExpr.Clause(delete.updates),
    )
}

internal abstract class UpdateClauseImpl(val action: UpdateAction) {
    private val _updates = mutableListOf<UpdateClauseExpr>()
    val updates: List<UpdateClauseExpr> get() = _updates

    fun AttributePath.update(value: Expression) {
        _updates += UpdateClauseExpr(action, this, value)
    }
}

internal class UpdateSetImpl :
    UpdateClauseImpl(UpdateAction.SET),
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

internal class UpdateRemoveImpl :
    UpdateClauseImpl(UpdateAction.REMOVE),
    UpdateDsl.Remove {

    override val attr = AttrImpl

    override fun AttributePath.unaryMinus() = update(LiteralExpr(null))
}

internal class UpdateAddImpl :
    UpdateClauseImpl(UpdateAction.ADD),
    UpdateDsl.Add {

    override val attr: Attr = AttrImpl

    override fun AttributePath.plusAssign(value: Expression) = update(value)
}

internal class UpdateDeleteImpl :
    UpdateClauseImpl(UpdateAction.DELETE),
    UpdateDsl.Delete {

    override val attr: Attr = AttrImpl

    override fun AttributePath.minusAssign(value: Expression) = update(value)
}
