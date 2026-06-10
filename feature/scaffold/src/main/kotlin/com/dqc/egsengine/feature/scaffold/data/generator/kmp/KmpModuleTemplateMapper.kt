/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.template.model.KmpModuleTemplateModel

/**
 * Builds [KmpModuleTemplateModel] for egs-kmp-template style feature modules (commonMain).
 */
internal fun SubProjectConfig.toKmpModuleTemplateModel(moduleName: String): KmpModuleTemplateModel {
    val pascal = moduleName.toPascalCase()
    val camel = pascal.replaceFirstChar { it.lowercase() }
    val normalized = moduleName.replace("-", "").replace("_", "")
    val packageName = "$basePackage.feature.$normalized"
    val pkgPath = packageName.replace('.', '/')
    val presentationPkg = "$packageName.presentation.$camel"
    val coreBase = "$basePackage.core.base"

    val pluginAlias = conventionPluginAliasFromConfig()

    return KmpModuleTemplateModel(
        pascal = pascal,
        camel = camel,
        packageName = packageName,
        basePackage = basePackage,
        namespace = packageName,
        pkgPath = pkgPath,
        conventionPluginAlias = pluginAlias,
        presentationPkg = presentationPkg,
        coreBase = coreBase,
    )
}

private fun SubProjectConfig.conventionPluginAliasFromConfig(): String = when (conventionPluginId) {
    null, "org.convention.cmp.feature" -> "cmp.feature.convention"
    "org.convention.cmp.feature.ui" -> "cmp.feature.ui.convention"
    "org.convention.cmp.feature.no.js" -> "cmp.feature.no.js.convention"
    else -> "cmp.feature.convention"
}

private fun String.toPascalCase(): String = split("-", "_").joinToString("") { part ->
    part.replaceFirstChar { it.uppercase() }
}
