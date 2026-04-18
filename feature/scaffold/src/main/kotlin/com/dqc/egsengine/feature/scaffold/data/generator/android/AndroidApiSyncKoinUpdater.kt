/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.domain.toModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * After `client api sync` for Android: handwritten [data.repository] impl, incremental Koin wiring,
 * and [generate.di] module integration (mirrors [com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiSyncKoinUpdater]).
 */
class AndroidApiSyncKoinUpdater {

    private val logger = LoggerFactory.getLogger(AndroidApiSyncKoinUpdater::class.java)

    fun applyAfterSync(
        subProjectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        if (config.platform != Platform.ANDROID) return

        val packageName = config.toModuleTemplate(moduleName).packageName
        val pascal = moduleName.androidModulePascalCase()
        val pkgPath = packageName.replace('.', '/')

        writeRepositoryImplIfAllowed(subProjectRoot, packageName, pascal, pkgPath, moduleName)
        wireRootFeatureModule(subProjectRoot, packageName, pascal, pkgPath, moduleName)
        patchDataModuleRepositoryImport(subProjectRoot, packageName, pascal, pkgPath, moduleName)
    }

    private fun writeRepositoryImplIfAllowed(
        subProjectRoot: File,
        packageName: String,
        pascal: String,
        pkgPath: String,
        moduleName: String,
    ) {
        val file = subProjectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt",
        )
        val newContent = renderHandwrittenRepositoryImpl(packageName, pascal)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(newContent)
            logger.info("Created handwritten {}RepositoryImpl at {}", pascal, file.path)
            return
        }

        val existing = file.readText()
        if (existing.contains(FREEZE_MARKER)) {
            logger.debug("Skip RepositoryImpl ({}): {}", file.path, FREEZE_MARKER)
            return
        }
        if (!shouldOverwriteRepositoryImpl(existing, pascal)) {
            logger.debug("Skip RepositoryImpl (hand-written content): {}", file.path)
            return
        }
        file.writeText(newContent)
        logger.info("Updated handwritten {}RepositoryImpl at {}", pascal, file.path)
    }

    private fun shouldOverwriteRepositoryImpl(text: String, pascal: String): Boolean {
        if (text.contains("override suspend fun getData(")) return true
        if (text.contains("egs-codegen: scaffold-repository-impl")) return true
        val legacyGen = "Generated${pascal}RepositorySupport"
        if (text.contains(legacyGen)) return true
        val gen = "Generated${pascal}ApiRepositorySupport"
        if (!text.contains(gen)) return false
        val m = Regex(
            """class\s+${Regex.escape(pascal)}RepositoryImpl\s*\(\s*([\s\S]*?)\)\s*:\s*${Regex.escape(gen)}""",
            RegexOption.MULTILINE,
        ).find(text) ?: return false
        val params = m.groupValues[1].trim()
        if (params.isEmpty()) return true
        return !params.contains(',')
    }

    private fun renderHandwrittenRepositoryImpl(packageName: String, pascal: String): String =
        """
        /*
         * Hand-written repository: extends generated API support.
         * egs-codegen: scaffold-repository-impl
         * Add // egs-sync:freeze on its own line to prevent api sync from overwriting this file.
         */
        package $packageName.data.repository

        import $packageName.generate.data.datasource.api.service.${pascal}RetrofitService
        import $packageName.generate.data.repository.Generated${pascal}ApiRepositorySupport

        internal class ${pascal}RepositoryImpl(
            service: ${pascal}RetrofitService,
        ) : Generated${pascal}ApiRepositorySupport(service) {
        }
        """.trimIndent() + "\n"

    private fun wireRootFeatureModule(
        subProjectRoot: File,
        packageName: String,
        pascal: String,
        pkgPath: String,
        moduleName: String,
    ) {
        val rootFile = subProjectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/$pkgPath/${pascal}KoinModule.kt",
        )
        if (!rootFile.exists()) {
            logger.debug("Android feature root Koin module not found (skip wire): {}", rootFile.path)
            return
        }

        val importData = "import $packageName.generate.di.generatedDataModule"
        val importDomain = "import $packageName.generate.di.generatedDomainModule"
        val legacyImportData = "import $packageName.generate.generatedDataModule"
        val legacyImportDomain = "import $packageName.generate.generatedDomainModule"

        val original = rootFile.readText()
        var text = original

        if (text.contains("generatedDataModule,") && text.contains("generatedDomainModule,")) {
            var t = text
            t = t.replace(legacyImportData, importData)
            t = t.replace(legacyImportDomain, importDomain)
            if (t != original) {
                rootFile.writeText(t)
                logger.info("Migrated generate imports to generate.di in {}", rootFile.path)
            }
            return
        }

        text = text.replace(legacyImportData, importData)
        text = text.replace(legacyImportDomain, importDomain)
        if (!text.contains(importData)) {
            text = insertAfterPackage(text, "$importData\n$importDomain\n")
        }

        val defaultListBlock = """
    dataModule,
    domainModule,
    presentationModule,
""".trimEnd()
        val legacyAndroidListBlock = """
    presentationModule,
    domainModule,
    dataModule,
""".trimEnd()
        val wiredListBlock = """
    generatedDataModule,
    dataModule,
    generatedDomainModule,
    domainModule,
    presentationModule,
""".trimEnd()
        var replaced = when {
            text.contains(defaultListBlock) -> text.replaceFirst(defaultListBlock, wiredListBlock)
            text.contains(legacyAndroidListBlock) -> text.replaceFirst(legacyAndroidListBlock, wiredListBlock)
            else ->
                Regex("""(\s*)dataModule\s*,\s*domainModule\s*,\s*presentationModule\s*,""").replace(text) { m ->
                    val ind = m.groupValues[1]
                    "${ind}generatedDataModule,\n${ind}dataModule,\n${ind}generatedDomainModule,\n${ind}domainModule,\n${ind}presentationModule,"
                }
        }
        if (replaced == text && !replaced.contains("generatedDataModule,")) {
            logger.warn(
                "Could not auto-wire generated modules into {} - add generatedDataModule / generatedDomainModule manually.",
                rootFile.path,
            )
            return
        }
        if (replaced != original) {
            rootFile.writeText(replaced)
            logger.info("Wired generated Koin modules into {}", rootFile.path)
        }
    }

    private fun patchDataModuleRepositoryImport(
        subProjectRoot: File,
        packageName: String,
        pascal: String,
        pkgPath: String,
        moduleName: String,
    ) {
        val file = subProjectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/$pkgPath/data/DataModule.kt",
        )
        if (!file.exists()) return

        var text = file.readText()
        val original = text
        val oldRepoImport = "import $packageName.domain.repository.${pascal}Repository"
        val newRepoImport = "import $packageName.generate.domain.repository.${pascal}Repository"
        if (text.contains(oldRepoImport) && !text.contains("generate.domain.repository.${pascal}Repository")) {
            text = text.replace(oldRepoImport, newRepoImport)
        }
        if (text != original) {
            file.writeText(text)
            logger.info("Pointed DataModule repository import at generate package: {}", file.path)
        }
    }

    private fun insertAfterPackage(text: String, insertion: String): String {
        val match = Regex("^package\\s+[^\\s]+", RegexOption.MULTILINE).find(text) ?: return text
        val insertAt = match.range.last + 1
        return text.substring(0, insertAt) + "\n\n" + insertion + text.substring(insertAt)
    }

    private companion object {
        const val FREEZE_MARKER = "// egs-sync:freeze"
    }
}

private fun String.androidModulePascalCase(): String =
    split("-", "_").joinToString("") { part ->
        part.replaceFirstChar { c -> c.uppercase() }
    }.ifBlank { "Feature" }
