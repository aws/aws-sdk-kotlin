/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.core

import aws.sdk.kotlin.runtime.InternalSdkApi

private const val INDENT = "    "

/**
 * An object which generates code into some context (typically an in-memory buffer which will eventually be written to a
 * file). It includes basic methods for writing code in Kotlin such as creating blocks and indentation, docs, and
 * ordinary lines of code.
 *
 * # String templates
 *
 * All methods which accept text to be written (e.g., [write], [openBlock], etc.) allow for the use of a string template
 * with zero or more arguments substituted in for placeholders. See [TemplateEngine] for more details on string
 * templates and argument substitution.
 *
 * # Indentation tracking
 *
 * Code generators track a running indentation level, which is used to form an indentation prefix prepended to lines
 * written by this generator. The default indentation string per level is 4 spaces and the indentation level starts at 0
 * for new generators. The indentation level may be increased or decreased manually via the methods [indent] and
 * [dedent]. The indentation level is also adjusted by **block** methods (e.g., [openBlock], [withBlock], etc.), which
 * automatically indent/dedent around logical blocks of code.
 */
@InternalSdkApi
public interface CodeGenerator {
    /**
     * The import directives for the current context
     */
    public val imports: ImportDirectives

    /**
     * Append a blank line if there isn't already one in the buffer (i.e., invoking this method multiple times
     * sequentially will append _only one_ blank line).
     */
    public fun blankLine()

    /**
     * Close a manually-opened block of code by dedenting and appending some finalizing text
     * @param template The string template or literal to append after dedenting (e.g., `}`). If this value is null, no
     * string will be appended after dedenting (i.e., the next write will occur immediately following the block).
     * @param args The arguments to substitute into the [template] (if any). See [TemplateEngine] for more details on
     * string templates and argument substitution.
     */
    public fun closeBlock(template: String?, vararg args: Any)

    /**
     * Close a manually-opened block and open a new one by dedenting, appending some intermediate text, and then
     * indenting again
     * @param template The string template or literal to append after dedenting and before re-indenting (e.g.,
     * `} else {`)
     * @param args The arguments to substitute into the [template] (if any). See [TemplateEngine] for more details on
     * string templates and argument substitution.
     */
    public fun closeAndOpenBlock(template: String, vararg args: Any)

    /**
     * Decreases the active indentation level
     * @param levels The number of levels to decrement. Defaults to `1` if unspecified.
     */
    public fun dedent(levels: Int = 1)

    /**
     * Formats the given template and arguments similar to [write] but returns the output as a string instead of writing
     * it to the buffer
     */
    public fun format(template: String, vararg args: Any): String

    /**
     * Increases the active indentation level
     * @param levels The number of levels to increment. Defaults to `1` if unspecified.
     */
    public fun indent(levels: Int = 1)

    /**
     * Open a block of code manually by appending some starting text and then indenting
     * @param template The string template or literal to append before indenting (e.g., `if (foo) {`)
     * @param args The arguments to substitute into the [template] (if any). See [TemplateEngine] for more details on
     * string templates and argument substitution.
     */
    public fun openBlock(template: String, vararg args: Any)

    /**
     * Sends the accumulated text of this generator to the backing buffer (e.g., writes it to a file)
     */
    public fun persist()

    /**
     * Writes a logical block of text by appending some starting text, indenting, executing the [block] function which
     * may add text inside the block, dedenting, and writing some finalizing text
     * @param preTemplate The string template or literal to append before indenting (e.g., `if (foo) {`)
     * @param postText The string literal to append after dedenting (e.g., `}`). No templating or argument substitution
     * will be performed on this string. If this value is null, no string will be appended after dedenting (i.e., the
     * next write will occur immediately following the block).
     * @param args The arguments to substitute into the [preTemplate] (if any). See [TemplateEngine] for more details on
     * string templates and argument substitution.
     * @param block A function to execute in between appending the [preTemplate] and before appending the [postText].
     * This function typically writes more code, which will inherit the active indentation level which will have been
     * incremented by `1` by this method.
     */
    public fun withBlock(preTemplate: String, postText: String?, vararg args: Any, block: () -> Unit)

    /**
     * Writes a block of documentation by automatically prepending KDoc-style comment tokens as prefixes. This method
     * will append an opening comment token (i.e., `/**`), add a special indentation prefix (i.e., ` * `) to the
     * existing indentation, execute the given [block], dedent back to the non-comment indentation prefix, and append a
     * closing comment token (i.e., `*/`).
     * @param block The function to execute in between appending the opening comment token and the closing comment
     * token. This function typically writes more code, which will inherit the active indentation prefix and be rendered
     * as a KDoc-style comment.
     */
    public fun withDocs(block: () -> Unit)

    /**
     * Writes a single line of documentation, wrapping it with KDoc-style comment tokens.
     * @param template The templated string of documentation to write
     * @param args The arguments to the templated string, if any
     */
    public fun docs(template: String, vararg args: Any)

    /**
     * Writes a newline
     */
    public fun newline() {
        write("")
    }

    /**
     * Writes a line of text, including a terminating newline (i.e., `\n`)
     * @param template The string template or literal to append
     * @param args The arguments to substitute into the [template] (if any). See [TemplateEngine] for more details on
     * string templates and argument substitution.
     */
    public fun write(template: String, vararg args: Any)

    /**
     * Writes a string of text, _not_ including a terminating newline. Invoking this method repeatedly in sequence
     * allows gradually forming an entire line incrementally rather than using a single [write] call. An in-progress
     * line may be terminated by invoking a method such as [write] (which will continue on the current line) or
     * [blankLine] (which will append a blank line).
     * @param template The string template or literal to append
     * @param args The arguments to substitute into the [template] (if any). See [TemplateEngine] for more details on
     * string templates and argument substitution.
     */
    public fun writeInline(template: String, vararg args: Any)
}

/**
 * The standard implementation of [CodeGenerator]. This implementation automatically prepends a comment indicating the
 * source is codegenned, a `package` directive specified by the constructor parameter [pkg], and a set of `import`
 * directives (if any) from [imports].
 *
 * Example of automatically prepended content:
 *
 * ```kotlin
 * // Code generated by dynamodb-mapper-ops-codegen. DO NOT EDIT!
 *
 * package foo.bar
 *
 * import a.A
 * import b.B
 * import c.C
 * ```
 * @param pkg The Kotlin package for the generated code (e.g., `aws.sdk.kotlin.hll.dynamodbmapper.operations`)
 * @param engine A configured template engine to use while processing string templates
 * @param persistCallback A callback method to invoke when the [persist] method is called on this class
 * @param imports The import directives for the generator. These may be appended by more lines being written.
 */
internal class CodeGeneratorImpl(
    private val pkg: String,
    private val engine: TemplateEngine,
    private val persistCallback: (String) -> Unit,
    override val imports: ImportDirectives = ImportDirectives(),
    private val codeGeneratorName: String,
) : CodeGenerator {
    private val builder = StringBuilder()
    private var indent = ""

    override fun blankLine() {
        if (!builder.endsWith("\n\n")) builder.append('\n')
    }

    override fun closeBlock(template: String?, vararg args: Any) {
        if (builder.endsWith("\n\n")) builder.deleteAt(builder.length - 1)

        dedent()
        if (template != null) write(template, *args)
    }

    override fun closeAndOpenBlock(template: String, vararg args: Any) {
        dedent()
        write(template, *args)
        indent()
    }

    override fun dedent(levels: Int) {
        repeat(levels) {
            indent = indent.removeSuffix(INDENT)
        }
    }

    override fun format(template: String, vararg args: Any) = engine.process(template, args.toList())

    override fun indent(levels: Int) {
        indent += INDENT.repeat(levels)
    }

    override fun openBlock(template: String, vararg args: Any) {
        write(template, *args)
        indent()
    }

    override fun persist() {
        val content = buildString {
            appendLine("// Code generated by $codeGeneratorName. DO NOT EDIT!")
            appendLine()
            appendLine("package $pkg")
            appendLine()
            imports.formatted.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            append(builder)
        }
        persistCallback(content)
    }

    override fun withBlock(preTemplate: String, postText: String?, vararg args: Any, block: () -> Unit) {
        openBlock(preTemplate, *args)
        block()
        closeBlock(postText)
    }

    override fun withDocs(block: () -> Unit) {
        write("/**")
        indent += " * "
        block()
        indent = indent.removeSuffix(" * ")
        write(" */")
    }

    override fun docs(template: String, vararg args: Any) = withDocs { write(template, *args) }

    override fun write(template: String, vararg args: Any) {
        writeInline(template, *args)
        builder.append('\n')
    }

    override fun writeInline(template: String, vararg args: Any) {
        val text = format(template, *args)
        if (builder.endsWith('\n')) builder.append(indent)
        builder.append(text)
    }
}
