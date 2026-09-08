/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

@InternalSdkApi
public object HllTypes {
    @InternalSdkApi
    public object SmithyKotlin {
        @InternalSdkApi
        public object RuntimeCore {
            public val GeneratedApi: TypeRef = TypeRef("aws.smithy.kotlin.runtime", "GeneratedApi")
        }
    }

    @InternalSdkApi
    public object MappingCore {
        @InternalSdkApi
        public object Converters {
            public val Converter: TypeRef = TypeRef("aws.sdk.kotlin.hll.mapping.core.converters", "Converter")
            public val ConverterChain: TypeRef = TypeRef("aws.sdk.kotlin.hll.mapping.core.converters", "ConverterChain")
        }
    }
}
