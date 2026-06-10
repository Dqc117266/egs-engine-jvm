/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.model.PageTemplateModel
import java.io.File

class PageKotlinTemplateRenderer(
    private val engine: TemplateEngine,
    private val template: PageTemplate,
    /** e.g. `src/main/kotlin` or `src/commonMain/kotlin` (KMP). */
    private val kotlinRootRel: String = "src/main/kotlin",
) {
    private val m: PageTemplateModel = template.toPageTemplateModel()

    fun renderContract(projectRoot: File? = null): String = engine.render("android/page/PageContract.kt.ftl", m, projectRoot)

    fun renderViewModel(projectRoot: File? = null): String = engine.render("android/page/PageViewModel.kt.ftl", m, projectRoot)

    fun renderScreen(projectRoot: File? = null): String = engine.render("android/page/PageScreen.kt.ftl", m, projectRoot)

    fun pathContract(): String = "$kotlinRootRel/${m.screenPkg.replace(".", "/")}/${m.pascalName}Contract.kt"

    fun pathViewModel(): String = "$kotlinRootRel/${m.screenPkg.replace(".", "/")}/${m.pascalName}ViewModel.kt"

    fun pathScreen(): String = "$kotlinRootRel/${m.screenDirPkg.replace(".", "/")}/${m.pascalName}Screen.kt"
}
