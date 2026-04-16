/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.template.TemplateEngine
import java.io.File

/**
 * Renders KMP screen sources using FreeMarker under `templates/kmp/page/`.
 * Layout: `src/commonMain/kotlin/<pkg>/presentation/screen/<camelPage>/бн`
 */
internal class KmpPageTemplateRenderer(
    private val engine: TemplateEngine,
    private val template: PageTemplate,
    private val kotlinRootRel: String,
) {
    private val m: Map<String, Any?> = template.toKmpPageTemplateMap()
    private val pkgPath: String = template.modulePackage.replace(".", "/")
    private val camelPage: String = template.pageName.replaceFirstChar { it.lowercase() }
    private val pascalPage: String = template.pageName

    fun renderContract(projectRoot: File? = null): String =
        engine.render("kmp/page/PageContract.kt.ftl", m, projectRoot)

    fun renderViewModel(projectRoot: File? = null): String =
        engine.render("kmp/page/PageViewModel.kt.ftl", m, projectRoot)

    fun renderScreen(projectRoot: File? = null): String =
        engine.render("kmp/page/PageScreen.kt.ftl", m, projectRoot)

    fun pathContract(): String =
        "$kotlinRootRel/$pkgPath/presentation/screen/$camelPage/${pascalPage}Contract.kt"

    fun pathViewModel(): String =
        "$kotlinRootRel/$pkgPath/presentation/screen/$camelPage/${pascalPage}ViewModel.kt"

    fun pathScreen(): String =
        "$kotlinRootRel/$pkgPath/presentation/screen/$camelPage/${pascalPage}Screen.kt"
}
