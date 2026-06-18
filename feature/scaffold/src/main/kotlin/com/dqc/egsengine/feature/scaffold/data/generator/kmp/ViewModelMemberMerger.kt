/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.common.ViewModelMergeSnippet
import com.dqc.egsengine.feature.scaffold.data.kotlin.KotlinMemberInspector

internal data class MergeStats(
    var ctorParams: Int = 0,
    var intents: Int = 0,
    var stateFields: Int = 0,
    var registerBlocks: Int = 0,
    var handlers: Int = 0,
    var contractImports: Int = 0,
    var viewModelImports: Int = 0,
)

internal data class MergeResult(
    val text: String,
    val stats: MergeStats,
)

/**
 * Idempotent string splice for KMP Contract / ViewModel — inserts only missing symbols.
 */
internal object ViewModelMemberMerger {
    fun mergeContract(
        contractText: String,
        snippets: List<ViewModelMergeSnippet>,
        existingIntentNames: Set<String>,
        existingStateNames: Set<String>,
        newContractImportLines: List<String>,
    ): MergeResult {
        val stats = MergeStats()
        var text = contractText
        val intentNames = existingIntentNames.toMutableSet()
        val stateNames = existingStateNames.toMutableSet()

        // Imports
        val existingImportSet = parseImportLines(text)
        val toAddImports = newContractImportLines.filter { it !in existingImportSet }.sorted()
        if (toAddImports.isNotEmpty()) {
            stats.contractImports = toAddImports.size
            text = insertImportLines(text, toAddImports)
        }

        // State fields (before ") : UiState")
        for (s in snippets) {
            val field = s.stateFieldText ?: continue
            val name = extractStatePropertyName(field) ?: continue
            if (name in stateNames) continue
            text = insertBeforeStateClosing(text, field)
            stateNames += name
            stats.stateFields++
        }

        // Intent members
        for (s in snippets) {
            if (s.intentMemberText.isBlank()) continue
            val intentName = extractIntentMemberName(s.intentMemberText) ?: continue
            if (intentName in intentNames) continue
            text = insertBeforeSealedIntentClose(text, s.intentMemberText)
            intentNames += intentName
            stats.intents++
        }

        return MergeResult(text = text, stats = stats)
    }

    fun mergeViewModel(
        vmText: String,
        pascalName: String,
        snippets: List<ViewModelMergeSnippet>,
        existingCtorTypes: Set<String>,
        existingRegisterBranches: Set<String>,
        existingHandlerNames: Set<String>,
    ): MergeResult {
        val stats = MergeStats()
        var text = vmText
        val classSimple = "${pascalName}ViewModel"
        val registerBranches = existingRegisterBranches.toMutableSet()
        val handlerNames = existingHandlerNames.toMutableSet()

        // Imports (batch)
        val existingImportSet = parseImportLines(text).toMutableSet()
        val vmImportsToAdd =
            snippets
                .flatMap { it.viewModelImportLines }
                .filter { it !in existingImportSet }
                .distinct()
                .sorted()
        if (vmImportsToAdd.isNotEmpty()) {
            stats.viewModelImports = vmImportsToAdd.size
            text = insertImportLines(text, vmImportsToAdd)
        }

        val ctorPresent = existingCtorTypes.toMutableSet()

        // Ctor params
        for (s in snippets) {
            val typeName = s.useCase.name
            if (typeName in ctorPresent) continue
            text = insertCtorParam(text, classSimple, s.ctorParamLine.trimEnd())
            ctorPresent += typeName
            stats.ctorParams++
        }

        // registerIntent blocks
        for (s in snippets) {
            if (s.registerIntentBlock.isBlank()) continue
            val branch = extractRegisterBranchName(s.registerIntentBlock) ?: continue
            if (branch in registerBranches) continue
            text = insertIntoRegisterIntents(text, s.registerIntentBlock)
            registerBranches += branch
            stats.registerBlocks++
        }

        // Handlers
        for (s in snippets) {
            val hf = s.handlerFunction ?: continue
            val hName = extractHandlerName(hf) ?: continue
            if (hName in handlerNames) continue
            text = insertBeforeViewModelClassClose(text, pascalName, hf)
            handlerNames += hName
            stats.handlers++
        }

        return MergeResult(text = text, stats = stats)
    }

    private fun parseImportLines(source: String): Set<String> = source
        .lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("import ") }
        .toSet()

    private fun insertImportLines(
        source: String,
        lines: List<String>,
    ): String {
        if (lines.isEmpty()) return source
        val block = KotlinMemberInspector.importsBlock(source)
        val toInsert = lines.joinToString("\n", postfix = "\n") { it.trim() }
        return if (block != null) {
            val before = source.substring(0, block.endExclusive)
            val after = source.substring(block.endExclusive)
            val needsNewline = !before.endsWith("\n")
            before + (if (needsNewline) "\n" else "") + toInsert + after
        } else {
            val pkg =
                Regex("""^package\s+[^\s]+\s*""", RegexOption.MULTILINE).find(source)?.value
                    ?: return source
            val idx = source.indexOf(pkg) + pkg.length
            source.substring(0, idx) + "\n\n" + toInsert + source.substring(idx)
        }
    }

    private fun insertBeforeStateClosing(
        contract: String,
        fieldSnippet: String,
    ): String {
        val marker =
            Regex("""\)\s*:\s*UiState""").find(contract)
                ?: return contract
        val insertAt = marker.range.first
        return contract.substring(0, insertAt) + fieldSnippet + contract.substring(insertAt)
    }

    private fun insertBeforeSealedIntentClose(
        contract: String,
        intentSnippet: String,
    ): String {
        val intentStart =
            Regex("""\bsealed\s+(?:class|interface)\s+Intent\b""").find(contract)?.range?.first
                ?: return contract
        val open = contract.indexOf('{', intentStart)
        val close = KotlinMemberInspector.findMatchingCloseBrace(contract, open) ?: return contract
        return contract.substring(0, close) + intentSnippet + "\n    " + contract.substring(close)
    }

    private fun insertCtorParam(
        vm: String,
        classSimple: String,
        paramLineWithoutComma: String,
    ): String {
        val classIdx = Regex("""\bclass\s+$classSimple\s*\(""").find(vm)?.range?.first ?: return vm
        val openParen = vm.indexOf('(', classIdx)
        val closeParen = KotlinMemberInspector.findMatchingCloseParen(vm, openParen) ?: return vm
        val inner = vm.substring(openParen + 1, closeParen).trim()
        val line = if (paramLineWithoutComma.endsWith(",")) paramLineWithoutComma else "$paramLineWithoutComma,"
        return if (inner.isEmpty()) {
            vm.substring(0, closeParen) + "\n$line\n" + vm.substring(closeParen)
        } else {
            val trimmed = inner.trimEnd()
            val withComma = if (trimmed.endsWith(",")) trimmed else "$trimmed,"
            vm.substring(0, openParen + 1) + "\n$withComma\n$line\n" + vm.substring(closeParen)
        }
    }

    private fun insertIntoRegisterIntents(
        vm: String,
        block: String,
    ): String {
        val m =
            Regex("""override\s+fun\s+registerIntents\s*\(\s*\)\s*\{""").find(vm)
                ?: return vm
        val openBrace = m.range.last
        require(vm[openBrace] == '{') { "registerIntents parse" }
        val closeBrace = findRegisterIntentsClosingBrace(vm, openBrace) ?: return vm
        val bodyForIndent = vm.substring(openBrace + 1, closeBrace)
        // [ \t]+ (not \s+) so the leading `\n` of `bodyForIndent` is not captured when the regex
        // matches at the start-of-input anchor — that would have made `indent` a newline + spaces
        // and corrupted the re-indent path in [normalizeRegisterIntentBlock].
        val indent =
            Regex("""(?m)^([ \t]+)registerIntent<""").find(bodyForIndent)?.groupValues?.get(1)
                ?: "        "
        val normalized = normalizeRegisterIntentBlock(block, indent)
        // Back up past any whitespace (including stray blank / trailing-space lines left over from
        // prior merges) so repeated runs don't accumulate blank lines before the closing brace.
        var insertAt = closeBrace
        while (insertAt > openBrace + 1 &&
            vm[insertAt - 1].let { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
        ) {
            insertAt--
        }
        val before = vm.substring(0, insertAt)
        val tail = vm.substring(insertAt)
        // Single newline when inserting into an empty registerIntents() body so we don't
        // introduce a leading blank line; otherwise keep one blank line between siblings.
        val sep = if (insertAt == openBrace + 1) "\n" else "\n\n"
        val insert = sep + normalized
        return before + insert + tail
    }

    /**
     * Closing `}` for [registerIntents][openBraceIndex] must not include braces from later
     * `private fun handle
` bodies. A plain global [findMatchingCloseBrace] scan can pair with
     * the first `}` inside the first handler when that handler is malformed (column 0) or when
     * nested lambdas confuse depth in edge cases. We bound the scan to the text before the first
     * handler method after [openBraceIndex].
     */
    private fun findRegisterIntentsClosingBrace(
        vm: String,
        openBraceIndex: Int,
    ): Int? {
        findMatchingCloseBraceInRange(vm, openBraceIndex, endExclusive = findFirstHandlerAfterRegisterIntents(vm, openBraceIndex))
            ?.let { return it }
        return KotlinMemberInspector.findMatchingCloseBrace(vm, openBraceIndex)
    }

    private fun findFirstHandlerAfterRegisterIntents(
        vm: String,
        openBraceIndex: Int,
    ): Int {
        val tail = vm.substring(openBraceIndex + 1)
        val re = Regex("""\r?\n(\s*)(?:private|internal)\s+fun\s+handle""")
        val match = re.find(tail) ?: return vm.length
        return openBraceIndex + 1 + match.range.first
    }

    private fun findMatchingCloseBraceInRange(
        source: String,
        openBraceIndex: Int,
        endExclusive: Int,
    ): Int? {
        if (openBraceIndex < 0 || openBraceIndex >= source.length) return null
        if (source[openBraceIndex] != '{') return null
        val end = endExclusive.coerceAtMost(source.length)
        var depth = 0
        var i = openBraceIndex
        while (i < end) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return null
    }

    /**
     * Produce a compact, uniformly-indented `registerIntent<
> {
     }` block. Blank lines are dropped
     * entirely – older builds of the engine sometimes emitted snippets with stray blank lines inside
     * the block, and repeated merges would then carry the extra blank lines forward.
     */
    private fun normalizeRegisterIntentBlock(
        block: String,
        indent: String,
    ): String {
        val nonBlank = block.lines().filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return ""
        val first = nonBlank.first()
        if (first.startsWith(indent)) return nonBlank.joinToString("\n")
        return nonBlank.joinToString("\n") { line ->
            val t = line.trim()
            when {
                t.startsWith("registerIntent") -> indent + t
                t == "}" -> indent + t
                else -> indent + "    " + t
            }
        }
    }

    private fun insertBeforeViewModelClassClose(
        vm: String,
        pascalName: String,
        handler: String,
    ): String {
        val braceIdx = findViewModelClassBodyOpenBrace(vm, pascalName) ?: return vm
        val classClose = KotlinMemberInspector.findMatchingCloseBrace(vm, braceIdx) ?: return vm
        val insertPos = findInsertionBeforeCompanionOrClassEnd(vm, braceIdx, classClose)
        val normalized = normalizePrivateHandlerFunctionIndent(handler)
        val before = vm.substring(0, insertPos)
        val sep =
            when {
                before.endsWith("\n\n") -> ""
                before.endsWith("\n") -> "\n"
                before.isEmpty() -> ""
                else -> "\n\n"
            }
        return before + sep + normalized + vm.substring(insertPos)
    }

    /**
     * Ensures the first line starts at class-member indent (4 spaces). Preserves **relative** nesting:
     * a flat `    ` + [trimStart] per line breaks bodies where [trimIndent] left `private fun` at column 0
     * but inner lines still had partial indent.
     */
    private fun normalizePrivateHandlerFunctionIndent(handler: String): String {
        val t = handler.trimEnd()
        val lines = t.lines()
        val nonBlank = lines.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return "$t\n"
        val minLead = nonBlank.minOfOrNull { line -> line.takeWhile { c -> c == ' ' }.length } ?: 0
        val first = nonBlank.first()
        if (minLead >= 4 &&
            (
                first.startsWith("    private ") ||
                    first.startsWith("    internal ") ||
                    first.startsWith("    protected ")
                )
        ) {
            return "$t\n"
        }
        val delta = 4 - minLead
        if (delta == 0) return "$t\n"
        return lines.joinToString("\n") { line ->
            if (line.isBlank()) {
                line
            } else {
                val n = line.takeWhile { c -> c == ' ' }.length
                " ".repeat(n + delta) + line.trimStart()
            }
        } + "\n"
    }

    /** New handlers go above a trailing [companion object], otherwise before the class closing [brace]. */
    private fun findInsertionBeforeCompanionOrClassEnd(
        vm: String,
        classOpenBrace: Int,
        classCloseBrace: Int,
    ): Int {
        val companionIdx = vm.lastIndexOf("companion object", classCloseBrace - 1)
        if (companionIdx <= classOpenBrace) return classCloseBrace
        val lineStart = vm.lastIndexOf('\n', companionIdx - 1).let { if (it < 0) 0 else it + 1 }
        return lineStart.coerceAtLeast(classOpenBrace + 1)
    }

    private fun findViewModelClassBodyOpenBrace(
        vm: String,
        pascalName: String,
    ): Int? {
        val re = Regex("""\)\s*\{\s*(\R\s*)override\s+fun\s+registerIntents""")
        val m = re.find(vm) ?: return null
        val rel = m.value.indexOf('{')
        return m.range.first + rel
    }
}

private fun extractStatePropertyName(fieldSnippet: String): String? {
    val m = Regex("""(?:override\s+)?val\s+(\w+)\s*:""").find(fieldSnippet) ?: return null
    return m.groupValues[1]
}

private fun extractIntentMemberName(intentSnippet: String): String? {
    Regex("""data\s+object\s+(\w+)\s*:""").find(intentSnippet)?.let { return it.groupValues[1] }
    Regex("""data\s+class\s+(\w+)\s*\(""").find(intentSnippet)?.let { return it.groupValues[1] }
    return null
}

private fun extractRegisterBranchName(block: String): String? {
    val m = Regex("""Intent\.(\w+)\s*>""").find(block) ?: return null
    return m.groupValues[1]
}

private fun extractHandlerName(handler: String): String? = Regex("""\bprivate\s+fun\s+(handle\w+|loadPage)\s*\(""").find(handler)?.groupValues?.get(1)
    ?: Regex("""\bprivate\s+suspend\s+fun\s+(handle\w+)\s*\(""").find(handler)?.groupValues?.get(1)
