/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.CloseableHttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

/**
 * Printable ASCII characters
 */
val PRINTABLE_CHARS = (32 until 127).map(Int::toChar).joinToString("")

data class HttpEngineContext(val name: String, val engine: CloseableHttpClientEngine)

/**
 * Run the [block] with each supported engine
 */
suspend fun withAllEngines(block: suspend (HttpEngineContext) -> Unit) {
    val contexts = listOf(
        HttpEngineContext("Platform default", DefaultHttpEngine()),
        HttpEngineContext("CRT", CrtHttpEngine()),
    )

    contexts.forEach { context ->
        context.engine.use {
            block(context)
        }
    }
}
