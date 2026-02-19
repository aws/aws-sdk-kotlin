/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

actual suspend fun withAllEngines(block: suspend (HttpClientEngine) -> Unit) {
    val engine = CrtHttpEngine()
    try {
        block(engine)
    } finally {
        engine.close()
    }
}
