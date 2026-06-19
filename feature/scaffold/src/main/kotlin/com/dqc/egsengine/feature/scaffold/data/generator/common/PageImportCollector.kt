/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

/** Shared import-collection helpers used by both Android and KMP page template mappers. */

internal fun contractShortTypeDisplay(typeFqn: String): String {
    val trimmed = typeFqn.trimEnd('?')
    val nullable = typeFqn.endsWith("?")
    val base = contractShortTypeDisplayInner(trimmed)
    return if (nullable) "$base?" else base
}

internal fun contractShortTypeDisplayInner(s: String): String {
    if (s.startsWith("List<") && s.endsWith(">")) {
        val inner = extractFirstGenericArgument(s, "List<") ?: return s
        return "List<${contractShortTypeDisplayInner(inner)}>"
    }
    val open = s.indexOf('<')
    if (open > 0 && s.endsWith(">")) {
        val args = s.substring(open + 1, s.length - 1)
        val outerShort = s.substring(0, open).substringAfterLast(".")
        val innerShort = splitTopLevelCommaGenericArgs(args).joinToString(", ") { contractShortTypeDisplayInner(it.trim()) }
        return "$outerShort<$innerShort>"
    }
    return if (s.contains(".")) s.substringAfterLast(".") else s
}

internal fun collectContractImportsForType(
    typeFqn: String,
    out: MutableSet<String>,
) {
    val trimmed = typeFqn.trimEnd('?')
    if (trimmed.startsWith("List<") && trimmed.endsWith(">")) {
        val inner = extractFirstGenericArgument(trimmed, "List<") ?: return
        collectContractImportsForType(inner, out)
        return
    }
    if (shouldEmitImportForFqn(trimmed)) {
        out.add("import $trimmed")
    }
}

internal fun shouldEmitImportForFqn(typeFqn: String): Boolean {
    if (!typeFqn.contains(".")) return false
    if (typeFqn.startsWith("kotlin.")) return false
    if (typeFqn.startsWith("java.")) return false
    return true
}

internal fun importLinesForKotlinTypeFqns(fqns: Iterable<String>): List<String> {
    val out = mutableSetOf<String>()
    fqns.forEach { collectContractImportsForType(it, out) }
    return out.sorted()
}

internal fun importLinesForUseCaseHandlerParams(parameters: List<com.dqc.egsengine.feature.templateengine.model.PageUseCaseParamModel>): List<String> = importLinesForKotlinTypeFqns(parameters.map { it.kotlinType })
