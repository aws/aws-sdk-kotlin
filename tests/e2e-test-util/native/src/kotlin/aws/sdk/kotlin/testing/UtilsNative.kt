/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

actual suspend fun withAllEngines(block: suspend (HttpEngineContext) -> Unit) {
    val engine = CrtHttpEngine()
    try {
        block(HttpEngineContext("CRT", engine))
    } finally {
        engine.close()
    }
}
