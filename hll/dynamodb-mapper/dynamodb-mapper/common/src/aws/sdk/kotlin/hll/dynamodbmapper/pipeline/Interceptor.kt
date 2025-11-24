/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemResponse
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest as LowLevelGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemResponse as LowLevelGetItemResponse

/**
 * An object which defines hooks that can execute at critical stages of the mapper request pipeline. Callers can use
 * these hooks to observe or modify the internal steps for executing a high-level operation.
 *
 * # Request pipeline
 *
 * The mapper request pipeline consists of 5 steps:
 * * **Initialization**: Setting up the operation pipeline and the gathering initial context
 * * **Serialization**: Converting high-level request objects (e.g., [GetItemRequest]) into low-level request objects
 * (e.g., [LowLevelGetItemRequest]), which includes converting high-level objects to DynamoDB items consisting of
 * attribute names and values
 * * **Low-level invocation**: Executing a low-level operation on the underlying [DynamoDbClient], such as
 * [DynamoDbClient.getItem]
 * * **Deserialization**: Converting low-level response objects (e.g., [LowLevelGetItemResponse]) into high-level
 * response objects (e.g., [GetItemResponse]), which includes converting DynamoDB items consisting of attributes names
 * and values into high-level objects
 * * **Completion**: Finalizing the high-level response to return to the caller or exception to throw
 *
 * # Hooks
 *
 * Hooks are interceptor methods which are invoked at stages before or after specific steps in the pipeline. They come
 * in two flavors: **read-only** and **modify** (or read-write). For example, [readBeforeInvocation] is a **read-only**
 * hook executed in the phase _before_ the **Low-level invocation** step of the pipeline.
 *
 * **Read-only hooks** are invoked before or after each step in the pipeline (except _before_ **Initialization** and
 * _after_ **Completion**). They offer a read-only view of a high-level operation in progress. They may be useful for
 * examining the state of an operation for logging, debugging, collecting metrics, etc. Each read-only hook receives a
 * context argument and returns [Unit].
 *
 * Any exception caught while executing a read-only hook will be added to the context and passed to subsequent
 * interceptors hooks in the same phase. Only after _all_ interceptors' read-only hooks for a given phase have completed
 * will any exception be thrown. For example, if a mapper has two interceptors **A** and **B** registered, and **A**'s
 * [readAfterSerialization] hook throws an exception, it will be added to the context passed to **B**'s
 * [readAfterSerialization] hook. After **B**'s [readAfterSerialization] hook has completed, the exception will be
 * thrown back to the caller.
 *
 * **Modify hooks** are invoked before each step in the pipeline (except _before_ **Initialization**). They offer the
 * ability to see and modify a high-level operation in progress. They can be used to customize behavior and data in ways
 * that mapper configuration and item schemas do not. Each modify hook receives a context argument and returns some
 * subset of that context as a result—either modified by the hook or passed-through from the input context.
 *
 * Any exception caught while executing a modify hook will halt the execution of other interceptors' modify hooks in the
 * same phase. The exception will be added to the context and passed to the next read-only hook. Once all interceptors'
 * read-only hooks for that phase have finished executing the exception will be thrown. For example, if a mapper has two
 * interceptors **A** and **B** registered, and **A**'s [modifyBeforeSerialization] hook throws an exception, **B**'s
 * [modifyBeforeSerialization] hook will not be invoked. Interceptors **A** and **B**'s [readAfterSerialization] hook
 * will execute, after which the exception will be thrown back to the caller.
 *
 * # Registration and execution order
 *
 * Interceptors are registered on [DynamoDbMapper] as configuration. Multiple interceptors may be registered on a single
 * mapper. The order in which interceptors are given in mapper config determines the order in which they will be
 * executed:
 * * For phases _before_ the **Low-level invocation** step, hooks will be executed _in given order_
 * * For phases _after_ the **Low-level invocation** step, hooks will be executed _in reverse order_
 *
 * @param T The type of item being serialized
 * @param HReq The type of high-level request object (e.g., [GetItemRequest])
 * @param LReq The type of low-level request object (e.g., [LowLevelGetItemRequest])
 * @param LRes The type of low-level response object (e.g., [LowLevelGetItemResponse])
 * @param HRes The type of high-level response object (e.g., [GetItemResponse])
 */
public interface Interceptor<T, HReq, LReq, LRes, HRes> {
    // Hooks functions are defined in the same order as pipeline execution below:

    // -----------------------------------------------------------------------------------------------------------------
    // Initialization step
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A **read-only** hook that runs _after_ the **Initialization** step
     * @param ctx Context containing the high-level request
     */
    public fun readAfterInitialization(ctx: HReqContext<T, HReq>) { }

    /**
     * A **modify** hook that runs _before_ the **Serialization** step
     * @param ctx Context containing the high-level request
     * @return A [SerializeInput] containing a potentially-modified high-level request and/or [ItemSchema]
     */
    public fun modifyBeforeSerialization(ctx: HReqContext<T, HReq>): SerializeInput<T, HReq> = ctx

    /**
     * A **read-only** hook that runs _before_ the **Serialization** step
     * @param ctx Context containing the high-level request
     */
    public fun readBeforeSerialization(ctx: HReqContext<T, HReq>) { }

    // -----------------------------------------------------------------------------------------------------------------
    // Serialization step
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A **read-only** hook that runs _after_ the **Serialization** step
     * @param ctx Context containing the high-level request and low-level request, which may be `null` if an exception
     * was caught during serialization
     */
    public fun readAfterSerialization(ctx: LReqContext<T, HReq, LReq?>) { }

    /**
     * A **modify** hook that runs _before_ the **Low-level invocation** step
     * @param ctx Context containing the high-level and low-level requests
     * @return A potentially-modified low-level request
     */
    public fun modifyBeforeInvocation(ctx: LReqContext<T, HReq, LReq>): LReq = ctx.lowLevelRequest

    /**
     * A **read-only** hook that runs _before_ the **Low-level invocation** step
     * @param ctx Context containing the high-level and low-level requests
     */
    public fun readBeforeInvocation(ctx: LReqContext<T, HReq, LReq>) { }

    // -----------------------------------------------------------------------------------------------------------------
    // Invocation step
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A **read-only** hook that runs _after_ the **Low-level invocation** step
     * @param ctx Context containing the high-level/low-level requests and the low-level response, which may be
     * `null` if an exception was caught during low-level invocation
     */
    public fun readAfterInvocation(ctx: LResContext<T, HReq, LReq, LRes?>) { }

    /**
     * A **modify** hook that runs _before_ the **Deserialization** step
     * @param ctx Context containing the high-level/low-level requests and the low-level response
     * @return A [DeserializeInput] containing a potentially-modified low-level response and/or [ItemSchema]
     */
    public fun modifyBeforeDeserialization(ctx: LResContext<T, HReq, LReq, LRes>): DeserializeInput<T, LRes> = ctx

    /**
     * A **read-only** hook that runs _before_ the **Deserialization** step
     * @param ctx Context containing the high-level/low-level requests and the low-level response
     */
    public fun readBeforeDeserialization(ctx: LResContext<T, HReq, LReq, LRes>) { }

    // -----------------------------------------------------------------------------------------------------------------
    // Deserialization step
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A **read-only** hook that runs _after_ the **Deserialization** step
     * @param ctx Context containing the high-level/low-level requests, the low-level response, and the high-level
     * response, which may be `null` if an exception was caught during deserialization
     */
    public fun readAfterDeserialization(ctx: HResContext<T, HReq, LReq, LRes, HRes?>) { }

    /**
     * A **modify** hook that runs _before_ the **Completion** step
     * @param ctx Context containing the high-level/low-level requests and responses
     * @return A potentially-modified high-level response
     */
    public fun modifyBeforeCompletion(ctx: HResContext<T, HReq, LReq, LRes, HRes>): HRes = ctx.highLevelResponse

    /**
     * A **read-only** hook that runs _before_ the **Completion** step
     * @param ctx Context containing the high-level/low-level requests and responses
     */
    public fun readBeforeCompletion(ctx: HResContext<T, HReq, LReq, LRes, HRes>) { }

    // -----------------------------------------------------------------------------------------------------------------
    // Completion step
    // -----------------------------------------------------------------------------------------------------------------
}

/**
 * A universal interceptor which acts on any type of high-level objects, requests, and responses
 */
public typealias InterceptorAny = Interceptor<*, *, *, *, *>
