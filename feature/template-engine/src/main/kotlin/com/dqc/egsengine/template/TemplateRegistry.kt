/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.template

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.cache.MultiTemplateLoader
import freemarker.cache.TemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import java.io.File

/**
 * Resolves `.ftl` templates with override chain (first match wins):
 * 1. `EGS_TEMPLATE_ROOT` env (dev: point at template-engine/resources/templates)
 * 2. `<projectRoot>/.egs/templates/`
 * 3. `~/.egs/templates/`
 * 4. Classpath `templates/` (bundled in JAR)
 */
class TemplateRegistry {

    fun configuration(projectRoot: File? = null): Configuration =
        Configuration(Configuration.VERSION_2_3_32).apply {
            defaultEncoding = "UTF-8"
            locale = java.util.Locale.ROOT
            setTemplateLoader(buildLoader(projectRoot))
        }

    fun getTemplate(name: String, projectRoot: File? = null): Template =
        configuration(projectRoot).getTemplate(name)

    internal fun buildLoader(projectRoot: File?): TemplateLoader {
        val loaders = mutableListOf<TemplateLoader>()
        resolveEnvTemplateRoot()?.let { loaders.add(FileTemplateLoader(it)) }
        projectRoot?.resolve(".egs/templates")?.takeIf { it.isDirectory }?.let {
            loaders.add(FileTemplateLoader(it))
        }
        val userTemplates = File(System.getProperty("user.home"), ".egs/templates")
        if (userTemplates.isDirectory) {
            loaders.add(FileTemplateLoader(userTemplates))
        }
        loaders.add(ClassTemplateLoader(TemplateRegistry::class.java.classLoader, "templates"))
        return MultiTemplateLoader(loaders.toTypedArray())
    }

    companion object {
        const val ENV_TEMPLATE_ROOT = "EGS_TEMPLATE_ROOT"

        fun resolveEnvTemplateRoot(): File? =
            System.getenv(ENV_TEMPLATE_ROOT)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { File(it) }
                ?.takeIf { it.isDirectory }
    }
}
