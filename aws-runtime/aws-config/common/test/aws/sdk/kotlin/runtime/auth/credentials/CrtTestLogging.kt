/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

/**
 * Initializes the CRT with the log level named by the `CRT_LOG_LEVEL` environment variable, if set. Must run before
 * anything else in the test binary initializes the CRT.
 */
internal expect fun initCrtLoggingFromEnv()
