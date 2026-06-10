/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.endpoint

import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

val benchmarkCompletion = Continuation<Endpoint>(EmptyCoroutineContext) { result ->
    result.getOrThrow()
}

inline fun resolveEndpointSync(completion: Continuation<Endpoint>, crossinline block: suspend () -> Endpoint): Endpoint {
    val result = suspend { block() }
        .startCoroutineUninterceptedOrReturn(completion)
    check(result !== COROUTINE_SUSPENDED) { "resolveEndpoint suspended unexpectedly" }
    @Suppress("UNCHECKED_CAST")
    return result as Endpoint
}
