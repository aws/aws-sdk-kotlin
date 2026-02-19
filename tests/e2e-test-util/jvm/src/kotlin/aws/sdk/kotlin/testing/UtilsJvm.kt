/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine

@OptIn(InternalApi::class)
actual suspend fun withAllEngines(block: suspend (HttpClientEngine) -> Unit) {
    val engines = listOf(
        OkHttpEngine(),
        OkHttp4Engine(),
        CrtHttpEngine(),
    )

    engines.forEach { engine ->
        engine.use {
            block(engine)
        }
    }
}
