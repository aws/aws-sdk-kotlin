/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.CloseableHttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

/**
 * Run the [block] with each supported engine
 */
actual suspend fun withAllEngines(block: suspend (HttpEngineContext) -> Unit) {
    val engines: List<Pair<String, CloseableHttpClientEngine>> = listOf(
        "Default" to DefaultHttpEngine(),
        "CRT" to CrtHttpEngine(),
    )

    engines.forEach { (name, engine) ->
        engine.use {
            block(HttpEngineContext(name, engine))
        }
    }
}
