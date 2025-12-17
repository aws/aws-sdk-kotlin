package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@InternalSdkApi
public object HllTypes {
    @InternalSdkApi
    public object SmithyKotlin {
        @InternalSdkApi
        public object RuntimeCore {
            public val GeneratedApi: TypeRef = TypeRef("aws.smithy.kotlin.runtime", "GeneratedApi")
        }
    }
}
