/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam

/**
 * Detects offset-style `Result<PageResult<T>>` vs `Flow<PagingData<T>>` for screen codegen.
 */
internal object PagePagingDetector {
    fun normalizePagingOption(raw: String): String = raw.lowercase().trim().ifEmpty { "auto" }

    /**
     * True when [returnType] is `Result<*>` wh**>` whose inner type is generic `PageResult<Item>` **or**
     * a concrete Swagger DTO `PageResultFoo` (e.g. `PageResultAppAiChatSessionRespVO`) with page + size params.
     */
    fun isOffsetPageResultUseCase(
        returnType: String?,
        pagingOption: String,
        parameters: List<UseCaseParam> = emptyList(),
    ): Boolean {
        if (normalizePagingOption(pagingOption) == "none") return false
        if (normalizePagingOption(pagingOption) == "paging3") return false
        val rt = returnType?.trim().orEmpty()
        if (rt.isEmpty()) return false
        val inner = extractResultInnerType(rt) ?: return false
        if (isGenericPageResultType(inner)) {
            if (normalizePagingOption(pagingOption) == "offset") return true
            if (normalizePagingOption(pagingOption) == "auto") return true
            return false
        }
        if (isConcretePageResultInner(inner) && hasPageAndSizeParams(parameters)) {
            if (normalizePagingOption(pagingOption) == "offset") return true
            if (normalizePagingOption(pagingOption) == "auto") return true
        }
        return false
    }

    /**
     * Swagger-style `PageResultSomething` (not `PageResult<**>` generic).
     */
    fun isConcretePageResultInner(innerResultType: String): Boolean {
        val s = innerResultType.trim()
        if (s.contains('<')) return false
        val simple = s.substringAfterLast('.')
        return CONCRETE_PAGE_RESULT_SIMPLE.matches(simple)
    }

    /**
     * Both a page-like and a size-like parameter must exist (not inferred defaults).
     */
    fun hasPageAndSizeParams(parameters: List<UseCaseParam>): Boolean {
        val names = parameters.map { it.name }
        val hasPage = names.any { it.equals("page", true) || it == "pageNo" || it.equals("pageIndex", true) }
        val hasSize = names.any { it.equals("pageSize", true) || it.equals("size", true) || it.equals("limit", true) }
        return hasPage && hasSize
    }

    /**
     * `PageResultAppAiChatSessionRespVO` ** `AppAiChatSessionRespVO` (simple name for [resolveParamTypeString]).
     */
    fun extractConcretePageResultItemSimpleName(innerResultType: String): String? {
        if (!isConcretePageResultInner(innerResultType)) return null
        val simple = innerResultType.trim().substringAfterLast('.')
        if (!simple.startsWith("PageResult")) return null
        val item = simple.removePrefix("PageResult")
        return item.takeIf { it.isNotEmpty() && item.first().isUpperCase() }
    }

    private val CONCRETE_PAGE_RESULT_SIMPLE = Regex("""^PageResult[A-Z]\w*$""")

    fun isPaging3FlowUseCase(
        returnType: String?,
        pagingOption: String,
    ): Boolean {
        val opt = normalizePagingOption(pagingOption)
        if (opt == "none") return false
        val rt = returnType?.trim().orEmpty()
        if (rt.isEmpty()) return false
        if (!looksLikeFlowReturn(rt)) return false
        if (opt == "paging3") return extractPagingDataItemType(rt) != null
        if (opt == "auto") return extractPagingDataItemType(rt) != null
        return false
    }

    fun looksLikeFlowReturn(returnType: String): Boolean {
        if (returnType.isBlank()) return false
        if (returnType.contains("kotlinx.coroutines.flow")) return true
        return FLOW_TYPE_INVOKE_REGEX.containsMatchIn(returnType)
    }

    private val FLOW_TYPE_INVOKE_REGEX =
        Regex("""\b(Flow|StateFlow|SharedFlow|MutableStateFlow|MutableSharedFlow)\s*<""")

    /**
     * Extracts `T` from `Flow<PagingData<T>>` (first match).
     */
    fun extractPagingDataItemType(returnType: String): String? {
        val m =
            Regex("""\bFlow\s*<\s*PagingData\s*<\s*([^>]+)\s*>""").find(returnType)
                ?: Regex("""\bFlow\s*<\s*[\w.]*\.?PagingData\s*<\s*([^>]+)\s*>""").find(returnType)
        return m
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    /**
     * `PageResult<**>` with a single type argument (generic), not `PageResultSomething` single identifier.
     */
    fun isGenericPageResultType(innerResultType: String): Boolean {
        val s = innerResultType.trim()
        val m = Regex("""^([\w.]*\.)?PageResult\s*<\s*([^>]+)\s*>$""").find(s) ?: return false
        val args = m.groupValues[2].trim()
        return args.isNotEmpty() && !args.contains(",")
    }

    /**
     * Returns the item type FQN/string inside `PageResult<Item>` (may be unqualified).
     */
    fun extractPageResultItemRaw(innerResultType: String): String? {
        val s = innerResultType.trim()
        val m = Regex("""^(?:[\w.]*\.)?PageResult\s*<\s*([^>]+)\s*>$""").find(s) ?: return null
        return m.groupValues[1].trim()
    }

    fun extractResultInnerType(returnType: String): String? {
        val idx = returnType.indexOf("Result<")
        if (idx < 0) return null
        var start = idx + "Result<".length
        var depth = 1
        var i = start
        while (i < returnType.length && depth > 0) {
            when (returnType[i]) {
                '<' -> depth++
                '>' -> depth--
            }
            i++
        }
        if (depth != 0) return null
        return returnType.substring(start, i - 1).trim()
    }

    data class PageParamNames(
        val page: String,
        val pageSize: String,
    )

    /**
     * Heuristic: `page`/`pageNo`/`pageIndex` + `pageSize`/`size`/`limit`.
     */
    fun detectPageParams(parameters: List<UseCaseParam>): PageParamNames {
        val names = parameters.map { it.name }
        val pageName =
            names.firstOrNull { it.equals("page", true) || it == "pageNo" || it.equals("pageIndex", true) }
                ?: "page"
        val sizeName =
            names.firstOrNull { it.equals("pageSize", true) || it.equals("size", true) || it.equals("limit", true) }
                ?: "pageSize"
        return PageParamNames(page = pageName, pageSize = sizeName)
    }
}
