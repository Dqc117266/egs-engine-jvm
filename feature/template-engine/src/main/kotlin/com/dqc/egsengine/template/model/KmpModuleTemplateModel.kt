/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.template.model

/**
 * Root model for KMP feature module FreeMarker templates (`templates/kmp/module/`).
 * Matches egs-kmp-template feature layout: commonMain, CMP convention, MVI under [presentationPkg].
 */
data class KmpModuleTemplateModel(
    val pascal: String,
    val camel: String,
    val packageName: String,
    val basePackage: String,
    /** Derived core base: e.g. "com.dqc.demo.core.base". */
    val coreBase: String,
    /** Android library namespace (CMP feature modules). */
    val namespace: String,
    /** Path under feature module: org/example/feature/todo */
    val pkgPath: String,
    /** Version-catalog plugin alias segment, e.g. `cmp.feature.convention` for `libs.plugins.cmp.feature.convention`. */
    val conventionPluginAlias: String,
    val presentationPkg: String,
)
