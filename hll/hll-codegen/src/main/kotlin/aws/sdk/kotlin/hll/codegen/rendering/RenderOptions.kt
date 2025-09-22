/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.codegen.rendering

import aws.smithy.kotlin.runtime.collections.AttributeKey

public object RenderOptions {

    /**
     * Determines the visibility of code-generated classes / objects. Defaults to [Visibility.PUBLIC].
     */
    public val VisibilityAttribute: AttributeKey<Visibility> = AttributeKey("Visibility")
}

/**
 * Determines the visibility of code-generated classes / objects. Defaults to [PUBLIC].
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
}
