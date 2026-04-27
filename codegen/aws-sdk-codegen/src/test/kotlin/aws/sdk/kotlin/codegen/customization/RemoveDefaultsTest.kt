/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import aws.smithy.kotlin.codegen.model.hasTrait
import aws.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.DefaultTrait
import kotlin.test.Test
import kotlin.test.assertFalse

class RemoveDefaultsTest {
    @Test
    fun removesDefaults() {
        val model = """
            ${"$"}version: "2.0"
            
            namespace test
            
            structure Foo {
                bar: Bar = 0
                baz: Integer = 0
            }
            
            @default(0)
            integer Bar
            
        """.toSmithyModel()

        val removeDefaultsFrom = setOf(ShapeId.from("test#Bar"), ShapeId.from("test#Foo\$baz"))
        val transformed = RemoveDefaults().removeDefaults(model, removeDefaultsFrom)
        val barMember = transformed.expectShape(ShapeId.from("test#Foo\$bar"))
        assertFalse(barMember.hasTrait<DefaultTrait>())
        val bazMember = transformed.expectShape(ShapeId.from("test#Foo\$baz"))
        assertFalse(bazMember.hasTrait<DefaultTrait>())
        val root = transformed.expectShape(ShapeId.from("test#Bar"))
        assertFalse(root.hasTrait<DefaultTrait>())
    }
}
