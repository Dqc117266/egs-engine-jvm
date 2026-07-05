/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.templateengine

import freemarker.cache.TemplateLoader
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

    /**
     * Render the first template in [candidateNames] that resolves against the loader chain,
     * e.g. a theme overlay `godot/metroidvania/enemy/generated.gd.ftl` falling back to the
     * base `godot/base/enemy/generated.gd.ftl`. Errors if none resolve.
     */
    fun renderWithFallback(
        candidateNames: List<String>,
        model: Any,
        projectRoot: File? = null,
    ): String {
        require(candidateNames.isNotEmpty()) { "No candidate template names supplied." }
        val cfg = registry.configuration(projectRoot)
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        val loader = cfg.templateLoader
        val name = candidateNames.firstOrNull { loader.exists(it) }
            ?: error("No template resolved; tried: $candidateNames")
        val template = cfg.getTemplate(name)
        val writer = StringWriter()
        template.process(model, writer)
        return writer.toString().trimEnd() + "\n"
    }

    private fun TemplateLoader.exists(name: String): Boolean = findTemplateSource(name) != null

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
