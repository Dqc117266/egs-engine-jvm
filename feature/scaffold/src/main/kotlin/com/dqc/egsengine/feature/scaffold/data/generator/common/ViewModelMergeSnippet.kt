/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo

/**
 * Incremental snippets for [com.dqc.egsengine.feature.scaffold.data.generator.kmp.ViewModelMemberMerger]
 * (KMP and Android page templates).
 */
data class ViewModelMergeSnippet(
    val useCase: UseCaseInfo,
    val intentMemberText: String,
    /** Including leading newline + indentation, or null if no field for this use case. */
    val stateFieldText: String?,
    /** All new imports for the ViewModel (use case, Result, etc.). */
    val viewModelImportLines: List<String>,
    val ctorParamLine: String,
    val registerIntentBlock: String,
    /** `null` when offset-paged body is handled only by [runPagedLoad] (KMP). */
    val handlerFunction: String?,
)
