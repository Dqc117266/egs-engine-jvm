/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PagePagingDetectorTest {

    @Test
    fun generic_page_result_auto() {
        val rt = "com.example.Result<PageResult<Foo>>"
        assertTrue(PagePagingDetector.isOffsetPageResultUseCase(rt, "auto"))
    }

    @Test
    fun generic_page_result_with_params() {
        val rt = "Result<PageResult<Bar>>"
        val params = listOf(UseCaseParam("pageNo", "Int"), UseCaseParam("pageSize", "Int"))
        assertTrue(PagePagingDetector.isOffsetPageResultUseCase(rt, "auto", params))
    }

    @Test
    fun concrete_page_result_with_page_and_size() {
        val rt = "Result<PageResultAppAiChatSessionRespVO>"
        val params = listOf(
            UseCaseParam("topicId", "Long?"),
            UseCaseParam("pageNo", "Int"),
            UseCaseParam("pageSize", "Int"),
        )
        assertTrue(PagePagingDetector.isOffsetPageResultUseCase(rt, "auto", params))
        assertTrue(PagePagingDetector.isConcretePageResultInner("PageResultAppAiChatSessionRespVO"))
        assertEquals("AppAiChatSessionRespVO", PagePagingDetector.extractConcretePageResultItemSimpleName("PageResultAppAiChatSessionRespVO"))
    }

    @Test
    fun concrete_missing_page_param_not_paged() {
        val rt = "Result<PageResultAppAiChatSessionRespVO>"
        val params = listOf(UseCaseParam("pageSize", "Int"))
        assertFalse(PagePagingDetector.isOffsetPageResultUseCase(rt, "auto", params))
    }

    @Test
    fun concrete_missing_size_param_not_paged() {
        val rt = "Result<PageResultAppAiChatSessionRespVO>"
        val params = listOf(UseCaseParam("pageNo", "Int"))
        assertFalse(PagePagingDetector.isOffsetPageResultUseCase(rt, "auto", params))
    }

    @Test
    fun not_result_wrapped() {
        assertFalse(PagePagingDetector.isOffsetPageResultUseCase("PageResult<Foo>", "auto"))
    }

    @Test
    fun paging_option_none_disables() {
        val rt = "Result<PageResult<Foo>>"
        assertFalse(PagePagingDetector.isOffsetPageResultUseCase(rt, "none"))
    }

    @Test
    fun paging_option_paging3_disables_offset() {
        val rt = "Result<PageResult<Foo>>"
        assertFalse(PagePagingDetector.isOffsetPageResultUseCase(rt, "paging3"))
    }

    @Test
    fun extract_item_raw_generic() {
        assertEquals("Foo", PagePagingDetector.extractPageResultItemRaw("PageResult<Foo>"))
    }

    @Test
    fun extract_concrete_item_null_for_generic() {
        assertNull(PagePagingDetector.extractConcretePageResultItemSimpleName("PageResult<Foo>"))
    }
}
