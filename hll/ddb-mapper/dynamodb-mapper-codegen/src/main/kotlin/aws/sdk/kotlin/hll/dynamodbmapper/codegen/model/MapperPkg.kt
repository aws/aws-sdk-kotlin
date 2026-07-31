/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.runtime.InternalSdkApi

@InternalSdkApi
public object MapperPkg {
    @InternalSdkApi
    public object Hl {
        public val Base: String = "aws.sdk.kotlin.hll.dynamodbmapper"
        public val Annotations: String = "$Base.annotations"
        public val Internal: String = "$Base.internal"
        public val Items: String = "$Base.items"
        public val Model: String = "$Base.model"
        public val PipelineImpl: String = "$Base.pipeline.internal"
        public val Values: String = "$Base.values"
        public val CollectionValues: String = "$Values.collections"
        public val ScalarValues: String = "$Values.scalars"
        public val SmithyTypeValues: String = "$Values.smithytypes"

        @InternalSdkApi
        public object Ops {
            public val Base: String = "${Hl.Base}.operations"
            public val Internal: String = "$Base.internal"
        }

        @InternalSdkApi
        public object Expressions {
            public val Base: String = "${Hl.Base}.expressions"
            public val Internal: String = "$Base.internal"
        }
    }

    @InternalSdkApi
    public object Ll {
        public val Base: String = "aws.sdk.kotlin.services.dynamodb"
        public val Model: String = "$Base.model"
    }
}
