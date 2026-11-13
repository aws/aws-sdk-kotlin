/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.codegen.rendering

import aws.smithy.kotlin.runtime.collections.AttributeKey

public object RenderOptions {

    /**
     * Determines the visibility of code-generated classes / objects
     */
    public val VisibilityAttribute: AttributeKey<Visibility> = AttributeKey("Visibility")
}

/**
 * Determines the visibility of code-generated classes / objects
 */
public enum class Visibility {
    /**
     * All code-generated constructs will be `public`
     */
    PUBLIC,

    /**
     * All code-generated constructs will be `internal`
     */
    INTERNAL,

    /**
     * Code-generated constructs will match the visibility of the user code they are generated from: `public` if every
     * user declaration named in their signatures is effectively public (it and every declaration enclosing it are
     * `public`), otherwise `internal`. Each code generator resolves this to [PUBLIC] or [INTERNAL] before rendering.
     */
    MATCH_SOURCE,
}
