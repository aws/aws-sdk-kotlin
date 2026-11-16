/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.smithy.kotlin.runtime.ErrorMetadata
import aws.smithy.kotlin.runtime.SdkBaseException

/** Marks this exception as one that retrying will not fix, and returns it so it can be thrown in place. */
internal fun <T : SdkBaseException> T.nonRecoverable(): T = apply {
    sdkErrorMetadata.attributes[ErrorMetadata.NonRecoverable] = true
}

/**
 * STS error codes that will not succeed on retry without a configuration or trust-policy change.
 *
 * Deliberately here and not in the cache: these codes are only meaningful for the two providers that call STS. A
 * cross-service set would also match a same-named error raised by an unrelated service, which is a different
 * condition with a different remedy.
 *
 * Both `RegionDisabled` and `RegionDisabledException` are present. The former is the documented code; the one STS
 * returns on the wire, and that the generated deserializer dispatches on, is the latter. Matching only the documented
 * spelling would classify a disabled region as recoverable and subject it to a 5-10 minute backoff.
 */
internal val NON_RECOVERABLE_STS_ERROR_CODES = setOf(
    "AccessDenied",
    "IDPRejectedClaim",
    "InvalidIdentityToken",
    "MalformedPolicyDocument",
    "PackedPolicyTooLarge",
    "RegionDisabled",
    "RegionDisabledException",
)
