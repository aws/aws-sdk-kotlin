/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.testutil

import aws.smithy.kotlin.codegen.test.TestModelDefault
import aws.smithy.kotlin.codegen.test.prependNamespaceAndService
import aws.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model

internal fun model(serviceName: String = TestModelDefault.SERVICE_NAME): Model =
    """
        @http(method: "PUT", uri: "/foo")
        operation Foo { }
        
        @http(method: "POST", uri: "/bar")
        operation Bar { }
    """
        .prependNamespaceAndService(operations = listOf("Foo", "Bar"), serviceName = serviceName)
        .toSmithyModel()
