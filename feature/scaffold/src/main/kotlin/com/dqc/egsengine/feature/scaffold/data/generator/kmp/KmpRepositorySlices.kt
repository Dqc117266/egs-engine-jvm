/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

/**
 * Which repository slices exist on disk for a KMP feature module (API / DB / Prefs).
 */
data class KmpRepositorySlices(
    val hasApi: Boolean,
    val hasDb: Boolean,
    val hasPrefs: Boolean,
)
