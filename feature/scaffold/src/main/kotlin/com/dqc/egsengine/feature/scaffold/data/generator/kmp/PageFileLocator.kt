/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import java.io.File

/**
 * Locates screen/fragment Contract / ViewModel paths under `presentation/{screen|fragment}/<camel>/`.
 */
internal object PageFileLocator {

    enum class PresentationLayout {
        Screen,
        Fragment,
    }

    data class ScreenKotlinPaths(
        val contract: File,
        val viewModel: File,
    )

    fun screenKotlinPaths(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
        kotlinRootRel: String,
        pageName: String,
        layout: PresentationLayout = PresentationLayout.Screen,
    ): ScreenKotlinPaths {
        val pascal = pageName.replaceFirstChar { it.uppercase() }
        val camel = pageName.replaceFirstChar { it.lowercase() }
        val pkgPath = modulePackage.replace(".", "/")
        val segment =
            when (layout) {
                PresentationLayout.Screen -> "screen"
                PresentationLayout.Fragment -> "fragment"
            }
        val base =
            projectRoot.resolve("feature/$moduleName/$kotlinRootRel/$pkgPath/presentation/$segment/$camel")
        return ScreenKotlinPaths(
            contract = base.resolve("${pascal}Contract.kt"),
            viewModel = base.resolve("${pascal}ViewModel.kt"),
        )
    }
}
