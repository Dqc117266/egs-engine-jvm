/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * [XRepositoryImpl] with Kotlin `by` delegation (Android `main`), same structure as KMP generator.
 */
class AndroidDbOnlyRepositoryImplGenerator {
    private val logger = LoggerFactory.getLogger(AndroidDbOnlyRepositoryImplGenerator::class.java)

    fun generateOrMerge(
        template: ModuleTemplate,
        subProjectRoot: File?,
        includeApi: Boolean,
        includeDb: Boolean,
        includePrefs: Boolean = false,
    ): GeneratedFile? {
        if (!includeApi && !includeDb && !includePrefs) return null
        val ctx = KmpSwaggerGeneratorContext(template)
        val pascal = ctx.pascalModuleName
        val pkg = ctx.rootPackage
        val moduleDir = "feature/${template.name}"
        val pkgPath = pkg.replace('.', '/')
        val path = "$moduleDir/src/main/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt"

        val content = renderContent(ctx, includeApi, includeDb, includePrefs)

        if (subProjectRoot != null) {
            val file = subProjectRoot.resolve(path)
            if (file.exists()) {
                val existing = file.readText()
                if (existing.contains(AndroidRepositoryCodegen.FREEZE_MARKER)) {
                    logger.debug("Skip Android RepositoryImpl (frozen): {}", file.path)
                    return null
                }
            }
        }

        return GeneratedFile(path, content)
    }

    private fun renderContent(
        ctx: KmpSwaggerGeneratorContext,
        includeApi: Boolean,
        includeDb: Boolean,
        includePrefs: Boolean,
    ): String {
        val pascal = ctx.pascalModuleName
        val pkg = ctx.rootPackage
        val moduleDatabaseName = "${pascal}Database"
        val dataSourceClass = "${moduleDatabaseName}DataSource"
        val imports = mutableListOf<String>()
        imports.add("$pkg.generate.domain.repository.${ctx.combinedRepositoryName}")
        val ctorParams = mutableListOf<String>()
        val delegates = mutableListOf<String>()
        if (includeApi) {
            imports.add("$pkg.generate.data.repository.${ctx.apiRepositorySupportName}")
            ctorParams.add("apiSupport: ${ctx.apiRepositorySupportName}")
            delegates.add("${ctx.apiRepositoryName} by apiSupport")
        }
        if (includeDb) {
            imports.add("$pkg.generate.data.repository.Generated${pascal}DbRepositorySupport")
            imports.add("$pkg.generate.data.datasource.database.$dataSourceClass")
            ctorParams.add("dbSupport: Generated${pascal}DbRepositorySupport")
            delegates.add("${dbRepositoryName(pascal)} by dbSupport")
        }
        if (includePrefs) {
            imports.add("$pkg.generate.data.repository.Generated${pascal}PrefsRepositorySupport")
            ctorParams.add("prefsSupport: Generated${pascal}PrefsRepositorySupport")
            delegates.add("${prefsRepositoryName(pascal)} by prefsSupport")
        }
        val importsBlock = imports.sorted().joinToString("\n") { "import $it" }
        val ctorBlock = ctorParams.joinToString(",\n    ")
        val delegateBlock = delegates.joinToString(",\n    ")
        return """
            /*
             * Hand-written repository: delegates to generated API/DB/Prefs support classes.
             * egs-codegen: scaffold-repository-impl-delegation
             * Add ${AndroidRepositoryCodegen.FREEZE_MARKER} on its own line to prevent overwrites.
             */
            package $pkg.data.repository

            $importsBlock

            internal class ${pascal}RepositoryImpl(
                $ctorBlock,
            ) : ${ctx.combinedRepositoryName},
                $delegateBlock {
            }
        """.trimIndent() + "\n"
    }

    private fun dbRepositoryName(pascal: String): String = "${pascal}DbRepository"

    private fun prefsRepositoryName(pascal: String): String = "${pascal}PrefsRepository"
}
