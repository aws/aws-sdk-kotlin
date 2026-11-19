/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.testing

import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine

/**
 * Printable ASCII characters
 */
val PRINTABLE_CHARS = (32 until 127).map(Int::toChar).joinToString("")

/**
 * A named HTTP client engine, used to identify which engine a test ran against
 * (e.g. for test-key suffixes and assertion messages).
 */
class HttpEngineContext(val name: String, val engine: HttpClientEngine)

/**
 * Run the [block] with each supported engine
 */
expect suspend fun withAllEngines(block: suspend (HttpEngineContext) -> Unit)
