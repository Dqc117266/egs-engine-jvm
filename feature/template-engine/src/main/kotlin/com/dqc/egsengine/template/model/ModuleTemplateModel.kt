/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.template.model

/** Base class FQNs for Android / KMP module scaffolding. */
data class BaseClassPackagesModel(
    val baseViewModel: String?,
    val baseFragment: String?,
    val resultClass: String?,
    val retrofitProvider: String?,
)

/** Freemarker root model for android module templates. */
data class ModuleTemplateModel(
    val pascal: String,
    val camel: String,
    val layoutSnakeName: String,
    val packageName: String,
    /** Use this in FTL (avoid is-prefixed names; FreeMarker treats them as method calls). */
    val android: Boolean,
    val basePackage: String?,
    val baseClasses: BaseClassPackagesModel,
    val uiContractPackage: String,
    val conventionPluginId: String?,
    val namespace: String?,
    /** Repository getData return type as Kotlin source text. */
    val repositoryReturnType: String,
    /** Extra imports for repository and impl. */
    val repositoryImports: List<String>,
    val repositoryImplImports: List<String>,
    val hasRetrofit: Boolean,
    val retrofitProviderFqcn: String?,
    val retrofitProviderSimpleName: String?,
    val hasBaseViewModel: Boolean,
    val baseViewModelImport: String?,
    val baseViewModelSimpleName: String?,
)
