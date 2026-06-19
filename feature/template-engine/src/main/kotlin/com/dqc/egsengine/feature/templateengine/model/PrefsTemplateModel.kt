/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.templateengine.model

/** Scalar field for prefs codegen (CLI fields without object key). */
data class PrefsScalarFieldModel(
    val storageKey: String,
    /** e.g. USER_ID for const val USER_ID */
    val constName: String,
    val kotlinType: String,
    val kotlinPropertyName: String,
    val defaultValueExpr: String,
)

/** Object snapshot for prefs codegen (CLI with --key). */
data class PrefsSnapshotModel(
    val objectKey: String,
    val keyConstName: String,
    val className: String,
    val defaultPropertyName: String,
    val fields: List<PrefsScalarFieldModel>,
)

/** Freemarker root model for prefs templates. */
data class PrefsTemplateModel(
    val featurePascal: String,
    val featureCamel: String,
    val packageName: String,
    val namespace: String,
    val scalars: List<PrefsScalarFieldModel>,
    val snapshots: List<PrefsSnapshotModel>,
    val listQueryKeys: List<PrefsScalarFieldModel>,
)
