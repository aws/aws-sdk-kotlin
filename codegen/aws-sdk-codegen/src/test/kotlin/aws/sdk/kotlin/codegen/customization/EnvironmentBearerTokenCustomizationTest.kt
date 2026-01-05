/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.smithy.kotlin.codegen.test.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvironmentBearerTokenCustomizationTest {
    @Test
    fun `test customization enabled for bedrock sigv4 signing name`() {
        val bedrockModel = """
            namespace com.test
            use aws.auth#sigv4
            use aws.api#service
            use smithy.api#httpBearerAuth
            
            @sigv4(name: "bedrock")
            @httpBearerAuth
            @service(sdkId: "Bedrock")
            service Bedrock {
                version: "1.0.0"
            }
        """.trimIndent().toSmithyModel()

        assertTrue {
            EnvironmentBearerTokenCustomization()
                .enabledForService(bedrockModel, bedrockModel.defaultSettings())
        }
    }

    fun `test customization enabled for bedrock sigv4 signing name with different sdkId`() {
        val bedrockRuntimeModel = """
            namespace com.test
            use aws.auth#sigv4
            use aws.api#service
            use smithy.api#httpBearerAuth
            
            @sigv4(name: "bedrock")
            @httpBearerAuth
            @service(sdkId: "Bedrock Runtime")
            service BedrockRuntime {
                version: "1.0.0"
            }
        """.trimIndent().toSmithyModel()

        assertTrue {
            EnvironmentBearerTokenCustomization()
                .enabledForService(bedrockRuntimeModel, bedrockRuntimeModel.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for non-bedrock sigv4 signing name`() {
        val nonBedrockModel = """
            namespace com.test
            use aws.auth#sigv4
            use aws.api#service
            use smithy.api#httpBearerAuth
            
            @sigv4(name: "s3")
            @httpBearerAuth
            @service(sdkId: "S3")
            service S3 {
                version: "1.0.0"
            }
        """.trimIndent().toSmithyModel()

        assertFalse {
            EnvironmentBearerTokenCustomization()
                .enabledForService(nonBedrockModel, nonBedrockModel.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for model without sigv4 trait`() {
        val noSigV4Model = """
            namespace com.test
            use aws.api#service
            use smithy.api#httpBearerAuth
            
            @service(sdkId: "NoSigV4")
            @httpBearerAuth
            service NoSigV4 {
                version: "1.0.0"
            }
        """.trimIndent().toSmithyModel()

        assertFalse {
            EnvironmentBearerTokenCustomization()
                .enabledForService(noSigV4Model, noSigV4Model.defaultSettings())
        }
    }

    @Test
    fun `test customization not enabled for model without bearer auth trait`() {
        val noBearerAuthModel = """
            namespace com.test
            use aws.auth#sigv4
            use aws.api#service
    
            @sigv4(name: "bedrock")
            @service(sdkId: "BedrockNoBearerAuth")
            service BedrockNoBearerAuth {
                version: "1.0.0"
            }
        """.trimIndent().toSmithyModel()

        assertFalse {
            EnvironmentBearerTokenCustomization()
                .enabledForService(noBearerAuthModel, noBearerAuthModel.defaultSettings())
        }
    }
}
