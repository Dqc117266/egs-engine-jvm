/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.model.BaseClassPackagesModel
import com.dqc.egsengine.template.model.ModuleTemplateModel

internal fun ModuleTemplate.toModuleTemplateModel(): ModuleTemplateModel {
    val pascal = name.toPascalCase()
    val camel = pascal.replaceFirstChar { it.lowercase() }
    val layoutSnakeName = pascal.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    val bcp = baseClassPackages
    val uiContractPackage =
        bcp.baseViewModel?.substringBeforeLast(".")
            ?: basePackage?.let { "$it.feature.base.presentation.viewmodel" }
            ?: "com.example.feature.base.presentation.viewmodel"

    val repositoryReturnType: String
    val repositoryImports: List<String>
    if (bcp.resultClass != null) {
        val simple = bcp.resultClass.substringAfterLast(".")
        repositoryReturnType = "$simple<String>"
        repositoryImports = listOf(bcp.resultClass)
    } else {
        repositoryReturnType = "kotlin.Result<String>"
        repositoryImports = emptyList()
    }

    val hasRetrofit = isAndroid && bcp.retrofitProvider != null
    val retrofitFqcn = bcp.retrofitProvider
    val retrofitSimple = retrofitFqcn?.substringAfterLast(".")

    val hasBaseViewModel = isAndroid && bcp.baseViewModel != null
    val baseVmFqcn = bcp.baseViewModel
    val baseVmSimple = baseVmFqcn?.substringAfterLast(".")

    val repositoryImplImports =
        buildList {
            add("$packageName.domain.repository.${pascal}Repository")
            repositoryImports.forEach { add(it) }
            if (hasRetrofit && retrofitFqcn != null) {
                add(retrofitFqcn)
            }
        }

    return ModuleTemplateModel(
        pascal = pascal,
        camel = camel,
        layoutSnakeName = layoutSnakeName,
        packageName = packageName,
        android = isAndroid,
        basePackage = basePackage,
        baseClasses =
        BaseClassPackagesModel(
            baseViewModel = bcp.baseViewModel,
            baseFragment = bcp.baseFragment,
            resultClass = bcp.resultClass,
            retrofitProvider = bcp.retrofitProvider,
        ),
        uiContractPackage = uiContractPackage,
        conventionPluginId = conventionPluginId,
        namespace = namespace,
        repositoryReturnType = repositoryReturnType,
        repositoryImports = repositoryImports,
        repositoryImplImports = repositoryImplImports,
        hasRetrofit = hasRetrofit,
        retrofitProviderFqcn = retrofitFqcn,
        retrofitProviderSimpleName = retrofitSimple,
        hasBaseViewModel = hasBaseViewModel,
        baseViewModelImport = baseVmFqcn,
        baseViewModelSimpleName = baseVmSimple,
    )
}

private fun String.toPascalCase(): String = split("-", "_").joinToString("") { part ->
    part.replaceFirstChar { it.uppercase() }
}
