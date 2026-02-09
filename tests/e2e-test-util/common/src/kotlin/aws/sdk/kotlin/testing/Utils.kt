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
 * Run the [block] with each supported engine
 */
expect suspend fun withAllEngines(block: suspend (HttpClientEngine) -> Unit)
