/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * An [ExpressionVisitor] that traverses an [Expression] to yield an expression string, map of attribute names, and map
 * of attribute values suitable for passing to DynamoDB. This visitor is designed for reuse across multiple expressions,
 * yielding multiple expression strings but unified maps of attribute names and values.
 *
 * For example:
 *
 * ```kotlin
 * val visitor = ParameterizingExpressionVisitor()
 *
 * ddb.query {
 *     filterExpression = visitor.visit(<Expression>)
 *     keyConditionExpression = visitor.visit(<Expression>)
 *     expressionAttributeNames = visitor.expressionAttributeNames()
 *     expressionAttributeValues = visitor.expressionAttributeValues()
 *     // …
 * }
 * ```
 */
internal open class ParameterizingExpressionVisitor : ExpressionVisitor<String> {
    private val namePlaceholders = mutableMapOf<String, String>()
    private val valuePlaceholders = mutableMapOf<AttributeValue, String>()

    private fun Expression.accept(): String = accept(this@ParameterizingExpressionVisitor)

    fun expressionAttributeNames(): Map<String, String>? = namePlaceholders
        .entries
        .associate { (name, placeholder) -> placeholder to name }
        .takeUnless { it.isEmpty() }

    fun expressionAttributeValues(): Map<String, AttributeValue>? = valuePlaceholders
        .entries
        .associate { (value, placeholder) -> placeholder to value }
        .takeUnless { it.isEmpty() }

    private fun funcString(
        funcName: String,
        initialOperand: Expression,
        additionalOperands: List<Expression>,
    ) = buildString {
        append(funcName)
        append('(')
        append(initialOperand.accept())
        additionalOperands.forEach { operand ->
            append(", ")
            append(operand.accept())
        }
        append(')')
    }

    override fun visit(expr: AdditiveExpr) = buildString {
        append(expr.left.accept())
        append(' ')
        append(expr.operation.exprString)
        append(' ')
        append(expr.right.accept())
    }

    override fun visit(expr: AndExpr) = expr.operands.joinToString(" AND ") { "(${it.accept()})" }

    override fun visit(expr: AttributePath) = buildString {
        expr.parent?.let { parent -> append(parent.accept()) }

        when (val element = expr.element) {
            is AttrPathElement.Index -> {
                append('[')
                append(element.index)
                append(']')
            }

            is AttrPathElement.Name -> {
                if (expr.parent != null) append('.')

                val literal = if (element.name.attributeNameNeedsEscaping) {
                    namePlaceholders.getOrPut(element.name) { "#k${namePlaceholders.size}" }
                } else {
                    element.name
                }
                append(literal)
            }
        }
    }

    override fun visit(expr: BetweenExpr) = buildString {
        append(expr.value.accept())
        append(" BETWEEN ")
        append(expr.min.accept())
        append(" AND ")
        append(expr.max.accept())
    }

    override fun visit(expr: BooleanFuncExpr) = funcString(expr.func.exprString, expr.path, expr.additionalOperands)

    override fun visit(expr: ComparisonExpr) = buildString {
        append(expr.left.accept())
        append(' ')
        append(expr.comparator.exprString)
        append(' ')
        append(expr.right.accept())
    }

    override fun visit(expr: LiteralExpr) = valuePlaceholders.getOrPut(expr.value) { ":v${valuePlaceholders.size}" }

    override fun visit(expr: InExpr): String = buildString {
        append(expr.value.accept())
        append(" IN (")
        expr.set.joinTo(this, ", ") { it.accept() }
        append(')')
    }

    override fun visit(expr: NotExpr) = "NOT (${expr.operand.accept()})"

    override fun visit(expr: OrExpr) = expr.operands.joinToString(" OR ") { "(${it.accept()})" }

    override fun visit(expr: ScalarFuncExpr) = funcString(
        expr.func.exprString,
        expr.initialOperand,
        expr.additionalOperands,
    )

    override fun visit(expr: UpdateExpr): String = buildString {
        fun appendClause(command: String, clause: UpdateExpr.Clause) {
            val updates = clause.updates
            if (updates.isNotEmpty()) {
                append(command)
                append(' ')
                updates.joinTo(this) { it.accept() }
                append(' ') // leave separator for later clauses
            }
        }

        appendClause("SET", expr.set)
        appendClause("REMOVE", expr.remove)
        appendClause("ADD", expr.add)
        appendClause("DELETE", expr.delete)
    }.trim()

    override fun visit(expr: UpdateClauseExpr) = buildString {
        append(expr.target.accept())

        if (expr.action != UpdateAction.REMOVE) {
            append(
                when (expr.action) {
                    UpdateAction.SET -> " = "
                    else -> " "
                },
            )

            append(expr.value.accept())
        }
    }
}

/**
 * The set of
 * [DynamoDB reserved words](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html)
 */
private val reservedWords = setOf(
    "abort",
    "absolute",
    "action",
    "add",
    "after",
    "agent",
    "aggregate",
    "all",
    "allocate",
    "alter",
    "analyze",
    "and",
    "any",
    "archive",
    "are",
    "array",
    "as",
    "asc",
    "ascii",
    "asensitive",
    "assertion",
    "asymmetric",
    "at",
    "atomic",
    "attach",
    "attribute",
    "auth",
    "authorization",
    "authorize",
    "auto",
    "avg",
    "back",
    "backup",
    "base",
    "batch",
    "before",
    "begin",
    "between",
    "bigint",
    "binary",
    "bit",
    "blob",
    "block",
    "boolean",
    "both",
    "breadth",
    "bucket",
    "bulk",
    "by",
    "byte",
    "call",
    "called",
    "calling",
    "capacity",
    "cascade",
    "cascaded",
    "case",
    "cast",
    "catalog",
    "char",
    "character",
    "check",
    "class",
    "clob",
    "close",
    "cluster",
    "clustered",
    "clustering",
    "clusters",
    "coalesce",
    "collate",
    "collation",
    "collection",
    "column",
    "columns",
    "combine",
    "comment",
    "commit",
    "compact",
    "compile",
    "compress",
    "condition",
    "conflict",
    "connect",
    "connection",
    "consistency",
    "consistent",
    "constraint",
    "constraints",
    "constructor",
    "consumed",
    "continue",
    "convert",
    "copy",
    "corresponding",
    "count",
    "counter",
    "create",
    "cross",
    "cube",
    "current",
    "cursor",
    "cycle",
    "data",
    "database",
    "date",
    "datetime",
    "day",
    "deallocate",
    "dec",
    "decimal",
    "declare",
    "default",
    "deferrable",
    "deferred",
    "define",
    "defined",
    "definition",
    "delete",
    "delimited",
    "depth",
    "deref",
    "desc",
    "describe",
    "descriptor",
    "detach",
    "deterministic",
    "diagnostics",
    "directories",
    "disable",
    "disconnect",
    "distinct",
    "distribute",
    "do",
    "domain",
    "double",
    "drop",
    "dump",
    "duration",
    "dynamic",
    "each",
    "element",
    "else",
    "elseif",
    "empty",
    "enable",
    "end",
    "equal",
    "equals",
    "error",
    "escape",
    "escaped",
    "eval",
    "evaluate",
    "exceeded",
    "except",
    "exception",
    "exceptions",
    "exclusive",
    "exec",
    "execute",
    "exists",
    "exit",
    "explain",
    "explode",
    "export",
    "expression",
    "extended",
    "external",
    "extract",
    "fail",
    "false",
    "family",
    "fetch",
    "fields",
    "file",
    "filter",
    "filtering",
    "final",
    "finish",
    "first",
    "fixed",
    "flattern",
    "float",
    "for",
    "force",
    "foreign",
    "format",
    "forward",
    "found",
    "free",
    "from",
    "full",
    "function",
    "functions",
    "general",
    "generate",
    "get",
    "glob",
    "global",
    "go",
    "goto",
    "grant",
    "greater",
    "group",
    "grouping",
    "handler",
    "hash",
    "have",
    "having",
    "heap",
    "hidden",
    "hold",
    "hour",
    "identified",
    "identity",
    "if",
    "ignore",
    "immediate",
    "import",
    "in",
    "including",
    "inclusive",
    "increment",
    "incremental",
    "index",
    "indexed",
    "indexes",
    "indicator",
    "infinite",
    "initially",
    "inline",
    "inner",
    "innter",
    "inout",
    "input",
    "insensitive",
    "insert",
    "instead",
    "int",
    "integer",
    "intersect",
    "interval",
    "into",
    "invalidate",
    "is",
    "isolation",
    "item",
    "items",
    "iterate",
    "join",
    "key",
    "keys",
    "lag",
    "language",
    "large",
    "last",
    "lateral",
    "lead",
    "leading",
    "leave",
    "left",
    "length",
    "less",
    "level",
    "like",
    "limit",
    "limited",
    "lines",
    "list",
    "load",
    "local",
    "localtime",
    "localtimestamp",
    "location",
    "locator",
    "lock",
    "locks",
    "log",
    "loged",
    "long",
    "loop",
    "lower",
    "map",
    "match",
    "materialized",
    "max",
    "maxlen",
    "member",
    "merge",
    "method",
    "metrics",
    "min",
    "minus",
    "minute",
    "missing",
    "mod",
    "mode",
    "modifies",
    "modify",
    "module",
    "month",
    "multi",
    "multiset",
    "name",
    "names",
    "national",
    "natural",
    "nchar",
    "nclob",
    "new",
    "next",
    "no",
    "none",
    "not",
    "null",
    "nullif",
    "number",
    "numeric",
    "object",
    "of",
    "offline",
    "offset",
    "old",
    "on",
    "online",
    "only",
    "opaque",
    "open",
    "operator",
    "option",
    "or",
    "order",
    "ordinality",
    "other",
    "others",
    "out",
    "outer",
    "output",
    "over",
    "overlaps",
    "override",
    "owner",
    "pad",
    "parallel",
    "parameter",
    "parameters",
    "partial",
    "partition",
    "partitioned",
    "partitions",
    "path",
    "percent",
    "percentile",
    "permission",
    "permissions",
    "pipe",
    "pipelined",
    "plan",
    "pool",
    "position",
    "precision",
    "prepare",
    "preserve",
    "primary",
    "prior",
    "private",
    "privileges",
    "procedure",
    "processed",
    "project",
    "projection",
    "property",
    "provisioning",
    "public",
    "put",
    "query",
    "quit",
    "quorum",
    "raise",
    "random",
    "range",
    "rank",
    "raw",
    "read",
    "reads",
    "real",
    "rebuild",
    "record",
    "recursive",
    "reduce",
    "ref",
    "reference",
    "references",
    "referencing",
    "regexp",
    "region",
    "reindex",
    "relative",
    "release",
    "remainder",
    "rename",
    "repeat",
    "replace",
    "request",
    "reset",
    "resignal",
    "resource",
    "response",
    "restore",
    "restrict",
    "result",
    "return",
    "returning",
    "returns",
    "reverse",
    "revoke",
    "right",
    "role",
    "roles",
    "rollback",
    "rollup",
    "routine",
    "row",
    "rows",
    "rule",
    "rules",
    "sample",
    "satisfies",
    "save",
    "savepoint",
    "scan",
    "schema",
    "scope",
    "scroll",
    "search",
    "second",
    "section",
    "segment",
    "segments",
    "select",
    "self",
    "semi",
    "sensitive",
    "separate",
    "sequence",
    "serializable",
    "session",
    "set",
    "sets",
    "shard",
    "share",
    "shared",
    "short",
    "show",
    "signal",
    "similar",
    "size",
    "skewed",
    "smallint",
    "snapshot",
    "some",
    "source",
    "space",
    "spaces",
    "sparse",
    "specific",
    "specifictype",
    "split",
    "sql",
    "sqlcode",
    "sqlerror",
    "sqlexception",
    "sqlstate",
    "sqlwarning",
    "start",
    "state",
    "static",
    "status",
    "storage",
    "store",
    "stored",
    "stream",
    "string",
    "struct",
    "style",
    "sub",
    "submultiset",
    "subpartition",
    "substring",
    "subtype",
    "sum",
    "super",
    "symmetric",
    "synonym",
    "system",
    "table",
    "tablesample",
    "temp",
    "temporary",
    "terminated",
    "text",
    "than",
    "then",
    "throughput",
    "time",
    "timestamp",
    "timezone",
    "tinyint",
    "to",
    "token",
    "total",
    "touch",
    "trailing",
    "transaction",
    "transform",
    "translate",
    "translation",
    "treat",
    "trigger",
    "trim",
    "true",
    "truncate",
    "ttl",
    "tuple",
    "type",
    "under",
    "undo",
    "union",
    "unique",
    "unit",
    "unknown",
    "unlogged",
    "unnest",
    "unprocessed",
    "unsigned",
    "until",
    "update",
    "upper",
    "url",
    "usage",
    "use",
    "user",
    "users",
    "using",
    "uuid",
    "vacuum",
    "value",
    "valued",
    "values",
    "varchar",
    "variable",
    "variance",
    "varint",
    "varying",
    "view",
    "views",
    "virtual",
    "void",
    "wait",
    "when",
    "whenever",
    "where",
    "while",
    "window",
    "with",
    "within",
    "without",
    "work",
    "wrapped",
    "write",
    "year",
    "zone",
)

private val String.attributeNameNeedsEscaping: Boolean
    get() = contains(attributeNameNeedsEscapingRegex)

private val attributeNameNeedsEscapingRegex = buildString {
    append('(')

    // Alternative 1: entire string is a reserved word
    reservedWords.joinTo(this, separator = "|", prefix = "^(", postfix = ")$")

    // OR
    append('|')

    // Alternative 2: string contains any non-alphanumeric character anywhere
    append("[^A-Za-z0-9]")

    append(')')
}.toRegex(RegexOption.IGNORE_CASE)
