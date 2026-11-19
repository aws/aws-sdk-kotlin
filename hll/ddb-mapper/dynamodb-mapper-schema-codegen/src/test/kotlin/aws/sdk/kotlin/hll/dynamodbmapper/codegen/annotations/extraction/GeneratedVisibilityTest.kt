/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.extraction

import aws.sdk.kotlin.hll.codegen.rendering.Visibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratedVisibilityTest {
    private val matchSource = testRenderContext(visibility = Visibility.MATCH_SOURCE)

    private val publicDecl = fakeClassDeclaration("org.example.User")
    private val internalDecl = fakeClassDeclaration("org.example.Secret", modifiers = setOf(Modifier.INTERNAL))

    @Test
    fun testMatchSourcePublicClass() {
        assertEquals(Visibility.PUBLIC, generatedVisibility(sequenceOf(publicDecl), matchSource))
    }

    @Test
    fun testMatchSourceInternalClass() {
        assertEquals(Visibility.INTERNAL, generatedVisibility(sequenceOf(internalDecl), matchSource))
    }

    @Test
    fun testMatchSourceAnyNonPublicExposedDeclaration() {
        assertEquals(Visibility.INTERNAL, generatedVisibility(sequenceOf(publicDecl, internalDecl), matchSource))
    }

    @Test
    fun testMatchSourceNothingExposed() {
        assertEquals(Visibility.PUBLIC, generatedVisibility(emptySequence(), matchSource))
    }

    @Test
    fun testMatchSourcePublicClassInInternalParent() {
        val parent = fakeClassDeclaration("org.example.Outer", modifiers = setOf(Modifier.INTERNAL))
        val decl = fakeClassDeclaration("org.example.Outer.User", parent = parent)
        assertEquals(Visibility.INTERNAL, generatedVisibility(sequenceOf(decl), matchSource))
    }

    @Test
    fun testMatchSourcePublicClassDeeplyNestedInInternalAncestor() {
        val root = fakeClassDeclaration("org.example.Root", modifiers = setOf(Modifier.INTERNAL))
        val middle = fakeClassDeclaration("org.example.Root.Middle", parent = root)
        val decl = fakeClassDeclaration("org.example.Root.Middle.User", parent = middle)
        assertEquals(Visibility.INTERNAL, generatedVisibility(sequenceOf(decl), matchSource))
    }

    @Test
    fun testMatchSourceJavaPackagePrivateClass() {
        val decl = fakeClassDeclaration("org.example.JavaUser", origin = Origin.JAVA)
        assertEquals(Visibility.INTERNAL, generatedVisibility(sequenceOf(decl), matchSource))
    }

    @Test
    fun testExplicitVisibilityIgnoresSource() {
        assertEquals(Visibility.PUBLIC, generatedVisibility(sequenceOf(internalDecl), testRenderContext(visibility = Visibility.PUBLIC)))
        assertEquals(Visibility.INTERNAL, generatedVisibility(sequenceOf(publicDecl), testRenderContext(visibility = Visibility.INTERNAL)))
    }

    @Test
    fun testReferencedDeclarationsIncludeTypeArguments() {
        val type = fakeKsType("kotlin.collections.Map", args = listOf(fakeKsType("kotlin.String"), fakeKsType("org.example.Secret", declaration = internalDecl)))
        assertEquals(
            listOf("kotlin.collections.Map", "kotlin.String", "org.example.Secret"),
            type.referencedDeclarations().map { it.qualifiedName!!.asString() }.toList(),
        )
    }

    @Test
    fun testReferencedDeclarationsSkipTypeParameters() {
        val typeParameter = mockk<KSType> {
            every { declaration } returns mockk<KSTypeParameter>()
            every { arguments } returns emptyList()
        }
        val type = fakeKsType("kotlin.collections.List", args = listOf(typeParameter))
        assertEquals(listOf("kotlin.collections.List"), type.referencedDeclarations().map { it.qualifiedName!!.asString() }.toList())
    }

    @Test
    fun testInnerClassIsRejected() {
        val diagnostics = DiagnosticCollector()
        val parent = fakeClassDeclaration("org.example.Outer")
        val decl = fakeClassDeclaration("org.example.Outer.Thing", modifiers = setOf(Modifier.INNER), parent = parent)

        assertFalse(validateNotInnerClass(decl, diagnostics))
        assertContains(diagnostics.diagnostics.single().message, "'org.example.Outer.Thing' is an inner class")
        assertTrue(validateNotInnerClass(parent, diagnostics))
    }

    @Test
    fun testPublicAndInternalClassesAreAccessible() {
        val diagnostics = DiagnosticCollector()
        assertTrue(validateAccessibleFromGeneratedCode(publicDecl, diagnostics))
        assertTrue(validateAccessibleFromGeneratedCode(internalDecl, diagnostics))
        assertEquals(emptyList(), diagnostics.diagnostics)
    }

    @Test
    fun testPrivateClassIsRejected() {
        assertInaccessible(fakeClassDeclaration("org.example.User", modifiers = setOf(Modifier.PRIVATE)), "'org.example.User' is private")
    }

    @Test
    fun testProtectedClassIsRejected() {
        val parent = fakeClassDeclaration("org.example.Outer")
        val decl = fakeClassDeclaration("org.example.Outer.User", modifiers = setOf(Modifier.PROTECTED), parent = parent)
        assertInaccessible(decl, "'org.example.Outer.User' is protected")
    }

    @Test
    fun testJavaPackagePrivateClassIsRejected() {
        assertInaccessible(fakeClassDeclaration("org.example.JavaUser", origin = Origin.JAVA), "'org.example.JavaUser' is package-private")
    }

    @Test
    fun testClassInPrivateParentIsRejected() {
        val parent = fakeClassDeclaration("org.example.Outer", modifiers = setOf(Modifier.PRIVATE))
        val decl = fakeClassDeclaration("org.example.Outer.User", parent = parent)
        assertInaccessible(decl, "'org.example.Outer.User' is nested in private 'org.example.Outer'")
    }

    @Test
    fun testClassDeeplyNestedInProtectedAncestorIsRejected() {
        val root = fakeClassDeclaration("org.example.Root")
        val middle = fakeClassDeclaration("org.example.Root.Middle", modifiers = setOf(Modifier.PROTECTED), parent = root)
        val decl = fakeClassDeclaration("org.example.Root.Middle.User", parent = middle)
        assertInaccessible(decl, "'org.example.Root.Middle.User' is nested in protected 'org.example.Root.Middle'")
    }

    private fun assertInaccessible(decl: KSClassDeclaration, expectedMessage: String) {
        val diagnostics = DiagnosticCollector()
        assertFalse(validateAccessibleFromGeneratedCode(decl, diagnostics))
        assertContains(diagnostics.diagnostics.single().message, expectedMessage)
    }
}
