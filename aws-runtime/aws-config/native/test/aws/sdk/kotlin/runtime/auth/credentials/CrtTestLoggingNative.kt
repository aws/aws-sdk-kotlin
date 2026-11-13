/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.LogDestination
import aws.sdk.kotlin.crt.LogLevel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun initCrtLoggingFromEnv() {
    val level = getenv("CRT_LOG_LEVEL")?.toKString()?.let(LogLevel::valueOf) ?: return
    CRT.initRuntime {
        logLevel = level
        // stderr is unbuffered, so lines survive the process being killed while hung
        logDestination = LogDestination.Stderr
    }
}
