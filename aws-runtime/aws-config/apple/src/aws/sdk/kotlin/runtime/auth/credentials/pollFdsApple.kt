/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.posix.poll
import platform.posix.pollfd

@OptIn(ExperimentalForeignApi::class)
internal actual fun pollFds(fds: CPointer<pollfd>, count: Int, timeoutMillis: Int): Int = poll(fds, count.convert(), timeoutMillis)
