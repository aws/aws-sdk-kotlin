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
    fun testTypeVar() {
        val pkg = "foo.bar"
        val imports = ImportDirectives()
        val processor = TypeProcessor(pkg, imports)

        assertEquals(TypeVar.T.shortName, processor.typeUsageProcessor.handler(TypeVar.T))
        assertEquals(0, imports.size)
    }

    @Test
    fun testTypeRefInSamePackage() {
        val pkg = "foo.bar"
        val imports = ImportDirectives()
        val processor = TypeProcessor(pkg, imports)

        val samePkgClass = TypeRef(pkg, "Apple")
        assertEquals("Apple", processor.typeUsageProcessor.handler(samePkgClass))
        assertEquals(0, imports.size)
    }

    @Test
    fun testTypeRefInAnotherPackage() {
        val pkg = "foo.bar"
        val imports = ImportDirectives()
        val processor = TypeProcessor(pkg, imports)

        val otherPkg = "bar.foo"
        val otherPkgClass = TypeRef(otherPkg, "Banana")
        assertEquals("Banana", processor.typeUsageProcessor.handler(otherPkgClass))
        assertEquals(1, imports.size)
        assertContains(imports, ImportDirective(otherPkgClass))

        // Try again
        assertEquals("Banana", processor.typeUsageProcessor.handler(otherPkgClass))
        assertEquals(1, imports.size) // Size shouldn't have changed since class is already imported
    }

    @Test
    fun testTypeRefWithArgs() {
        val pkg = "foo.bar"
        val otherPkg = "bar.foo"
        val imports = ImportDirectives()
        val processor = TypeProcessor(pkg, imports)

        val fig = TypeRef(otherPkg, "Fig")
        val elderberry = TypeRef(otherPkg, "Elderberry", genericArgs = listOf(TypeVar("E"), fig))
        val date = TypeRef(pkg, "Date")
        val cherry = TypeRef(otherPkg, "Cherry", genericArgs = listOf(date, elderberry))
        assertEquals("Cherry<Date, Elderberry<E, Fig>>", processor.typeUsageProcessor.handler(cherry))
        assertEquals(3, imports.size)
        assertContains(imports, ImportDirective(cherry))
        assertContains(imports, ImportDirective(elderberry))
        assertContains(imports, ImportDirective(fig))
    }
}
