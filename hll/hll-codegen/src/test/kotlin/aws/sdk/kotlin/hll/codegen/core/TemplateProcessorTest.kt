/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TemplateProcessorTest {
    @Test
    fun testLiteral() {
        val processor = TemplateProcessor.Literal
        assertEquals("foo", processor.handler("foo"))
    }

    @Test
    fun testQuotedString() {
        val processor = TemplateProcessor.QuotedString
        assertEquals(""""This is a test!"""", processor.handler("This is a test!"))
        assertEquals(""""This is a \"test\"!"""", processor.handler("""This is a "test"!"""))
    }

    @Test
    fun testType() {
        val pkg = "foo.bar"
        val imports = ImportDirectives()
        val processor = TemplateProcessor.forType(pkg, imports)

        val typeVar = TypeVar("T")
        assertEquals("T", processor.handler(typeVar))
        assertEquals(0, imports.size)

        val samePkgClass = TypeRef(pkg, "Apple")
        assertEquals("Apple", processor.handler(samePkgClass))
        assertEquals(0, imports.size)

        val otherPkg = "bar.foo"
        val otherPkgClass = TypeRef(otherPkg, "Banana")
        assertEquals("Banana", processor.handler(otherPkgClass))
        assertEquals(1, imports.size)
        assertContains(imports, ImportDirective(otherPkgClass))

        // Try again
        assertEquals("Banana", processor.handler(otherPkgClass))
        assertEquals(1, imports.size) // Size shouldn't have changed since class is already imported

        val fig = TypeRef(otherPkg, "Fig")
        val elderberry = TypeRef(otherPkg, "Elderberry", genericArgs = listOf(TypeVar("E"), fig))
        val date = TypeRef(pkg, "Date")
        val cherry = TypeRef(otherPkg, "Cherry", genericArgs = listOf(date, elderberry))
        assertEquals("Cherry<Date, Elderberry<E, Fig>>", processor.handler(cherry))
        assertEquals(4, imports.size)
        assertContains(imports, ImportDirective(cherry))
        assertContains(imports, ImportDirective(elderberry))
        assertContains(imports, ImportDirective(fig))
    }
}
