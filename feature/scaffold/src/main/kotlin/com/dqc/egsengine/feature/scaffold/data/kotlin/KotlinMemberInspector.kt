/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.kotlin

/**
 * Lightweight Kotlin source inspection for idempotent ViewModel / Contract merging.
 * Uses brace/paren depth ¡ª not a full parser.
 */
object KotlinMemberInspector {
    data class CtorParam(
        val name: String,
        val type: String,
    )

    data class ImportBlock(
        val startIndex: Int,
        val endExclusive: Int,
    )

    /**
     * Primary constructor parameters for [classSimpleName], e.g. `FooViewModel`.
     */
    fun parsePrimaryConstructorParams(
        source: String,
        classSimpleName: String,
    ): List<CtorParam> {
        val classIdx = findClassDeclarationIndex(source, classSimpleName) ?: return emptyList()
        val openParen = source.indexOf('(', classIdx)
        if (openParen < 0) return emptyList()
        val closeParen = findMatchingCloseParen(source, openParen) ?: return emptyList()
        val inner = source.substring(openParen + 1, closeParen)
        if (inner.isBlank()) return emptyList()
        return splitTopLevelCommas(inner).mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val m =
                Regex("""^\s*private\s+val\s+(\w+)\s*:\s*(.+)$""").find(trimmed)
                    ?: Regex("""^\s*val\s+(\w+)\s*:\s*(.+)$""").find(trimmed)
            if (m == null) return@mapNotNull null
            val name = m.groupValues[1]
            val type = m.groupValues[2].trim().trimEnd(',')
            CtorParam(name = name, type = type)
        }
    }

    /**
     * Member names inside `sealed class Intent` / `sealed interface Intent` (`data object` / `data class`).
     */
    fun sealedIntentMemberNames(source: String): Set<String> {
        val intentIdx =
            Regex("""\bsealed\s+(?:class|interface)\s+Intent\b""").find(source)?.range?.first
                ?: return emptySet()
        val open = source.indexOf('{', intentIdx)
        if (open < 0) return emptySet()
        val close = findMatchingCloseBrace(source, open) ?: return emptySet()
        val body = source.substring(open + 1, close)
        val names = mutableSetOf<String>()
        Regex("""\bdata\s+object\s+(\w+)\s*:""").findAll(body).forEach { names += it.groupValues[1] }
        Regex("""\bdata\s+class\s+(\w+)\s*\(""").findAll(body).forEach { names += it.groupValues[1] }
        return names
    }

    /**
     * Property names in `data class State(` ¡­ `)`.
     */
    fun dataClassPropertyNames(
        source: String,
        dataClassName: String = "State",
    ): Set<String> {
        val marker = Regex("""\bdata\s+class\s+$dataClassName\s*\(""").find(source) ?: return emptySet()
        val openParen = marker.range.last
        val closeParen = findMatchingCloseParen(source, openParen) ?: return emptySet()
        val inner = source.substring(openParen + 1, closeParen)
        val names = mutableSetOf<String>()
        Regex("""\bval\s+(\w+)\s*:""").findAll(inner).forEach { names += it.groupValues[1] }
        return names
    }

    /**
     * `registerIntent<¡­Contract.Intent.Name>` ¡ú `Name`.
     */
    fun registerIntentBranchNames(
        source: String,
        contractSimpleName: String,
    ): Set<String> {
        val re = Regex("""registerIntent\s*<\s*$contractSimpleName\s*Contract\.Intent\.(\w+)\s*>""")
        return re.findAll(source).map { it.groupValues[1] }.toSet()
    }

    /** `private fun handleFoo` ¡ú `handleFoo` */
    fun privateHandlerFunctionNames(source: String): Set<String> {
        val names = mutableSetOf<String>()
        Regex("""\bprivate\s+fun\s+(handle\w+)\s*\(""").findAll(source).forEach { names += it.groupValues[1] }
        Regex("""\bprivate\s+suspend\s+fun\s+(handle\w+)\s*\(""").findAll(source).forEach { names += it.groupValues[1] }
        return names
    }

    /**
     * Span of import lines in [source], using character indices into [source] (not [String.lines]).
     *
     * [String.lines] plus a fixed `+1` per line breaks under CRLF (`\r\n`): each logical line adds **two**
     * characters, so [ImportBlock.endExclusive] could land **inside** an import string and split it when
     * new imports are spliced in.
     */
    fun importsBlock(source: String): ImportBlock? {
        val importLine = Regex("""(?m)^import\s+.*$""")
        var firstStart: Int? = null
        var lastEndExclusive: Int? = null
        for (m in importLine.findAll(source)) {
            if (firstStart == null) firstStart = m.range.first
            var end = m.range.last + 1
            if (end < source.length && source[end] == '\r') end++
            if (end < source.length && source[end] == '\n') end++
            lastEndExclusive = end
        }
        if (firstStart == null || lastEndExclusive == null) return null
        return ImportBlock(startIndex = firstStart, endExclusive = lastEndExclusive)
    }

    fun findMatchingCloseParen(
        source: String,
        openParenIndex: Int,
    ): Int? {
        if (openParenIndex < 0 || openParenIndex >= source.length) return null
        if (source[openParenIndex] != '(') return null
        return findClosing(source, openParenIndex, '(', ')')
    }

    fun findMatchingCloseBrace(
        source: String,
        openBraceIndex: Int,
    ): Int? {
        if (openBraceIndex < 0 || openBraceIndex >= source.length) return null
        if (source[openBraceIndex] != '{') return null
        return findClosing(source, openBraceIndex, '{', '}')
    }

    private fun findClassDeclarationIndex(
        source: String,
        classSimpleName: String,
    ): Int? {
        val re = Regex("""\b(?:internal|public|private)\s+class\s+$classSimpleName\b|\bclass\s+$classSimpleName\b""")
        return re.find(source)?.range?.first
    }

    private fun findClosing(
        source: String,
        start: Int,
        open: Char,
        close: Char,
    ): Int? {
        var depth = 0
        var i = start
        while (i < source.length) {
            when (source[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return null
    }

    private fun splitTopLevelCommas(s: String): List<String> {
        val out = mutableListOf<String>()
        var depthAngle = 0
        var depthParen = 0
        var start = 0
        var i = 0
        while (i <= s.length) {
            val c = s.getOrNull(i)
            if (c == null || (c == ',' && depthAngle == 0 && depthParen == 0)) {
                val part = s.substring(start, i).trim()
                if (part.isNotEmpty()) out.add(part)
                start = i + 1
            } else {
                when (c) {
                    '<' -> depthAngle++
                    '>' -> depthAngle--
                    '(' -> depthParen++
                    ')' -> depthParen--
                }
            }
            i++
        }
        return out
    }
}
