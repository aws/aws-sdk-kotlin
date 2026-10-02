/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Named constants for various mapper runtime packages
 */
@InternalSdkApi
public object Pkg {
    @InternalSdkApi
    public object Kotlin {
        public val Base: String = "kotlin"
        public val Collections: String = "$Base.collections"
        public val Jvm: String = "$Base.jvm"
    }

    @InternalSdkApi
    public object Java {
        public val Base: String = "java"
        public val Math: String = "$Base.math"
    }

    @InternalSdkApi
    public object Kotlinx {
        public val Base: String = "kotlinx"

        @InternalSdkApi
        public object Coroutines {
            public val Base: String = "${Kotlinx.Base}.coroutines"
            public val Flow: String = "$Base.flow"
        }
    }
}
