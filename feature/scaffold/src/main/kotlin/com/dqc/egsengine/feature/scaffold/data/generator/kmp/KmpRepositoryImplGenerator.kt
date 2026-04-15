/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Scaffolds [TodoRepositoryImpl] with Kotlin `by` delegation to each generated support class.
 */
class KmpRepositoryImplGenerator {

    private val logger = LoggerFactory.getLogger(KmpRepositoryImplGenerator::class.java)

    companion object {
        const val FREEZE_MARKER = "// egs-sync:freeze"
    }

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
        val path = "$moduleDir/src/commonMain/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt"

        val content = renderContent(ctx, includeApi, includeDb, includePrefs)

        if (subProjectRoot != null) {
            val file = subProjectRoot.resolve(path)
            if (file.exists()) {
                val existing = file.readText()
                if (existing.contains(FREEZE_MARKER)) {
                    logger.debug("Skip RepositoryImpl (frozen): {}", file.path)
                    return null
                }
            }
        }

        return GeneratedFile(path, content)
    }

    /**
     * Kotlin source for [renderContent]; used by API sync when refreshing delegation wiring.
     */
    fun renderDelegationRepositoryImpl(
        template: ModuleTemplate,
        includeApi: Boolean,
        includeDb: Boolean,
        includePrefs: Boolean,
    ): String {
        val ctx = KmpSwaggerGeneratorContext(template)
        return renderContent(ctx, includeApi, includeDb, includePrefs)
    }

    fun shouldPatchForDelegation(existingText: String, pascal: String): Boolean {
        if (existingText.contains(FREEZE_MARKER)) return false
        if (existingText.contains("egs-codegen: scaffold-repository-impl-delegation")) return true
        if (existingText.contains("egs-codegen: scaffold-repository-impl")) return true
        if (existingText.contains("egs-codegen: db-only-repository-impl")) return true
        val legacy = "Generated${pascal}RepositorySupport"
        if (existingText.contains(legacy)) return true
        return false
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
         * Add $FREEZE_MARKER on its own line to prevent overwrites.
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
