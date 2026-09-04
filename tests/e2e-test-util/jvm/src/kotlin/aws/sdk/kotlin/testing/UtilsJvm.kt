/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.http.engine.CloseableHttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine

/**
 * Run the [block] with each supported engine
 */
@OptIn(InternalApi::class)
actual suspend fun withAllEngines(block: suspend (HttpEngineContext) -> Unit) {
    val engines: List<Pair<String, CloseableHttpClientEngine>> = listOf(
        "OkHttp" to OkHttpEngine(),
        "OkHttp4" to OkHttp4Engine(),
        "CRT" to CrtHttpEngine(),
    )

    engines.forEach { (name, engine) ->
        engine.use {
            block(HttpEngineContext(name, engine))
        }
    }
}
