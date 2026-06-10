/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.template

import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.io.StringWriter

/**
 * Renders FreeMarker templates to strings or files.
 *
 * @param templateName path relative to template roots, e.g. `android/module/KoinModule.kt.ftl`
 */
class TemplateEngine(
    private val registry: TemplateRegistry,
) {
    fun render(
        templateName: String,
        model: Any,
        projectRoot: File? = null,
    ): String {
        val cfg = registry.configuration(projectRoot)
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        val template = cfg.getTemplate(templateName)
        val writer = StringWriter()
        template.process(model, writer)
        return writer.toString().trimEnd() + "\n"
    }

    fun renderToFile(
        templateName: String,
        model: Any,
        outputFile: File,
        projectRoot: File? = null,
    ) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(render(templateName, model, projectRoot))
    }
}
