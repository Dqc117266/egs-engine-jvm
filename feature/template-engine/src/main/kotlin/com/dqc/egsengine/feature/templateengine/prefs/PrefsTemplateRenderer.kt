/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.templateengine.prefs

import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.model.PrefsTemplateModel
import java.io.File

/** Freemarker prefs templates for future egs client prefs codegen. */
class PrefsTemplateRenderer(
    private val engine: TemplateEngine,
) {
    fun renderPrefsKeys(
        model: PrefsTemplateModel,
        projectRoot: File? = null,
    ): String = engine.render("prefs/PrefsKeys.kt.ftl", model, projectRoot)

    fun renderPrefsDefaults(
        model: PrefsTemplateModel,
        projectRoot: File? = null,
    ): String = engine.render("prefs/PrefsDefaults.kt.ftl", model, projectRoot)

    fun renderPrefsDataSource(
        model: PrefsTemplateModel,
        projectRoot: File? = null,
    ): String = engine.render("prefs/PrefsDataSource.kt.ftl", model, projectRoot)
}
