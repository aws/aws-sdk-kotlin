/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.ec2

import aws.smithy.kotlin.codegen.model.expectShape
import aws.smithy.kotlin.codegen.model.isNullable
import aws.smithy.kotlin.codegen.test.newTestContext
import aws.smithy.kotlin.codegen.test.prependNamespaceAndService
import aws.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.StructureShape
import kotlin.test.Test
import kotlin.test.assertTrue

class EC2MakePrimitivesOptionalTest {
    @Test
    fun testNullability() {
        val model = """
            operation Foo {
                input: FooInput
            }
            
            structure FooInput {
                @required
                @default(0)
                int: PrimitiveInteger,
                @default(false)
                bool: PrimitiveBoolean,
                other: Other,
                @default(1)
                defaultInt: Integer
                @required
                requiredString: String
            }
            
            structure Other {}
            
        """.prependNamespaceAndService(version = "2", operations = listOf("Foo")).toSmithyModel()

        val ctx = model.newTestContext()
        val transformed = EC2MakePrimitivesOptional().preprocessModel(model, ctx.generationCtx.settings)

        // get the synthetic input which is the one that will be transformed
        val struct = transformed.expectShape<StructureShape>("smithy.kotlin.synthetic.test#FooRequest")
        struct.members().forEach {
            val memberSymbol = ctx.generationCtx.symbolProvider.toSymbol(it)
            assertTrue(memberSymbol.isNullable)
        }
    }
}
