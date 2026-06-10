/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

/**
 * Idempotent patches when merging a screen to offset-paged [PagingListState] (incremental `add use case`).
 */
internal object ContractPagingMergePatcher {
    fun patchIfNeeded(
        contractText: String,
        pascalName: String,
        pagedItemContractRef: String,
    ): String {
        if (pagedItemContractRef.isBlank()) return contractText
        val withHeader = patchStateHeader(contractText, pagedItemContractRef)
        return insertCopyPagingIfMissing(withHeader, pascalName, pagedItemContractRef)
    }

    private fun patchStateHeader(
        contractText: String,
        itemRef: String,
    ): String {
        val key = "data class State("
        val ds = contractText.indexOf(key)
        if (ds < 0) return contractText
        val head = contractText.substring(0, ds)
        var tail = contractText.substring(ds)
        if (tail.contains("PagingListState<")) return contractText
        tail =
            tail.replaceFirst(
                Regex("""\)\s*:\s*UiState(?!\s*,\s*PagingListState)"""),
                ") : UiState, PagingListState<$itemRef>",
            )
        return head + tail
    }

    private fun insertCopyPagingIfMissing(
        contractText: String,
        pascalName: String,
        pagedItemContractRef: String,
    ): String {
        if (contractText.contains("override fun copyPaging(")) return contractText
        val key = "data class State("
        val ds = contractText.indexOf(key)
        if (ds < 0) return contractText
        val tail = contractText.substring(ds)
        val m =
            Regex("""\)\s*:\s*UiState(?:\s*,\s*PagingListState<[^>]+>)?\s*\{""")
                .find(tail) ?: return contractText
        val openBraceIdx = ds + m.range.last
        val insertAt = openBraceIdx + 1
        val body = copyPagingBody(pascalName, pagedItemContractRef)
        return contractText.substring(0, insertAt) + "\n" + body + contractText.substring(insertAt)
    }

    private fun copyPagingBody(
        pascalName: String,
        itemRef: String,
    ): String =
        """
        |
        |        override fun copyPaging(
        |            items: List<$itemRef>,
        |            total: Long,
        |            page: Int,
        |            pageSize: Int,
        |            isRefreshing: Boolean,
        |            isLoadingMore: Boolean,
        |            endReached: Boolean,
        |            pagingError: Throwable?,
        |        ): ${pascalName}Contract.State = copy(
        |            items = items,
        |            total = total,
        |            page = page,
        |            pageSize = pageSize,
        |            isRefreshing = isRefreshing,
        |            isLoadingMore = isLoadingMore,
        |            endReached = endReached,
        |            pagingError = pagingError,
        |        )
        """.trimMargin()
}
