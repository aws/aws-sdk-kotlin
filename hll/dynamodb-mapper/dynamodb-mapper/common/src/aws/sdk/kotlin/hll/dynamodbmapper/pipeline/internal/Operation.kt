/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.InterceptorAny

internal data class Operation<T, S : ItemSchema<T>, HReq, LReq, LRes, HRes>(
    val initialize: (HReq) -> HReqContextImpl<T, S, HReq>,
    val serialize: (HReq, S) -> LReq,
    val lowLevelInvoke: suspend (LReq) -> LRes,
    val deserialize: (LRes, S) -> HRes,
    val interceptors: List<Interceptor<T, S, HReq, LReq, LRes, HRes>>,
) {
    constructor(
        initialize: (HReq) -> HReqContextImpl<T, S, HReq>,
        serialize: (HReq, S) -> LReq,
        lowLevelInvoke: suspend (LReq) -> LRes,
        deserialize: (LRes, S) -> HRes,
        interceptors: Collection<InterceptorAny>,
    ) : this(
        initialize,
        serialize,
        lowLevelInvoke,
        deserialize,
        interceptors.map {
            // Will cause runtime ClassCastExceptions during interceptor invocation if the types don't match. Is that ok?
            @Suppress("UNCHECKED_CAST")
            it as Interceptor<T, S, HReq, LReq, LRes, HRes>
        },
    )

    suspend fun execute(hReq: HReq): HRes {
        val hReqContext = doInitialize(hReq)
        val lReqContext = doSerialize(hReqContext)
        val lResContext = doLowLevelInvoke(lReqContext)
        val hResContext = doDeserialize(lResContext)
        return finalize(hResContext)
    }

    private fun <I : ErrorCombinable<I>> readOnlyHook(
        input: I,
        reverse: Boolean = false,
        hook: Interceptor<T, S, HReq, LReq, LRes, HRes>.(I) -> Unit,
    ) = interceptors.fold(input, reverse) { ctx, interceptor ->
        runCatching {
            interceptor.hook(ctx)
        }.fold(
            onSuccess = { ctx },
            onFailure = { e -> ctx + e },
        )
    }.apply { error?.let { throw it } } // Throw error if present after executing all read-only hooks

    private fun <I, V> modifyHook(
        input: I,
        reverse: Boolean = false,
        hook: Interceptor<T, S, HReq, LReq, LRes, HRes>.(I) -> V,
    ): I where I : Combinable<I, V>, I : ErrorCombinable<I> {
        var latestCtx = input
        return runCatching {
            interceptors.fold(latestCtx, reverse) { ctx, interceptor ->
                latestCtx = ctx
                val value = interceptor.hook(ctx)
                ctx + value
            }
        }.fold(
            onSuccess = { it },
            onFailure = { e -> latestCtx + e },
        )
    }

    private fun doInitialize(input: HReq): HReqContextImpl<T, S, HReq> {
        val ctx = initialize(input)
        return readOnlyHook(ctx) { readAfterInitialization(it) }
    }

    private fun doSerialize(inputCtx: HReqContextImpl<T, S, HReq>): LReqContextImpl<T, S, HReq, LReq> {
        val rbsCtx = modifyHook(inputCtx) { modifyBeforeSerialization(it) }
        val serCtx = readOnlyHook(rbsCtx) { readBeforeSerialization(it) }

        val serRes = serCtx.runCatching { serialize(serCtx.highLevelRequest, serCtx.serializeSchema) }
        val lReq = serRes.getOrNull()
        val rasCtx = serCtx + serRes.exceptionOrNull() + lReq

        return readOnlyHook(rasCtx) { readAfterSerialization(it) }.solidify()
    }

    private suspend fun doLowLevelInvoke(
        inputCtx: LReqContextImpl<T, S, HReq, LReq>,
    ): LResContextImpl<T, S, HReq, LReq, LRes> {
        val rbiCtx = modifyHook(inputCtx) { modifyBeforeInvocation(it) }
        val invCtx = readOnlyHook(rbiCtx) { readBeforeInvocation(it) }

        val invRes = runCatching { lowLevelInvoke(invCtx.lowLevelRequest) }
        val lRes = invRes.getOrNull()
        val raiCtx = invCtx + invRes.exceptionOrNull() + lRes

        return readOnlyHook(raiCtx, reverse = true) { readAfterInvocation(it) }.solidify()
    }

    private fun doDeserialize(
        inputCtx: LResContextImpl<T, S, HReq, LReq, LRes>,
    ): HResContextImpl<T, S, HReq, LReq, LRes, HRes> {
        val rbdCtx = modifyHook(inputCtx, reverse = true) { modifyBeforeDeserialization(it) }
        val desCtx = readOnlyHook(rbdCtx, reverse = true) { readBeforeDeserialization(it) }

        val desRes = desCtx.runCatching { deserialize(desCtx.lowLevelResponse, desCtx.deserializeSchema) }
        val hRes = desRes.getOrNull()
        val radCtx = desCtx + desRes.exceptionOrNull() + hRes

        return readOnlyHook(radCtx, reverse = true) { readAfterDeserialization(it) }.solidify()
    }

    private fun finalize(inputCtx: HResContextImpl<T, S, HReq, LReq, LRes, HRes>): HRes {
        val raeCtx = modifyHook(inputCtx, reverse = true) { modifyBeforeCompletion(it) }
        val finalCtx = readOnlyHook(raeCtx, reverse = true) { readBeforeCompletion(it) }
        return finalCtx.highLevelResponse!!
    }
}

private inline fun <T, R> List<T>.fold(initial: R, reverse: Boolean, operation: (R, T) -> R): R =
    if (reverse) foldRight(initial) { curr, acc -> operation(acc, curr) } else fold(initial, operation)
