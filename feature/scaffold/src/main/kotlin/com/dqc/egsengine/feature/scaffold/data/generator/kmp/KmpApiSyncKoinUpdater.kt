/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.feature.scaffold.domain.toModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * After `client api sync` for KMP: handwritten [data.repository] impl, incremental Koin wiring,
 * and [generate.di] module integration.
 */
class KmpApiSyncKoinUpdater(
    private val kmpFeatureBuildGradleUpdater: KmpFeatureBuildGradleUpdater,
    private val kmpRepositoryImplGenerator: KmpRepositoryImplGenerator,
) {

    private val logger = LoggerFactory.getLogger(KmpApiSyncKoinUpdater::class.java)

    fun applyAfterSync(
        subProjectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        if (config.platform !in setOf(Platform.KMP, Platform.KMP_ANDROID)) return

        val template = config.toModuleTemplate(moduleName)
        val packageName = template.packageName
        val pascal = moduleName.kmpModulePascalCase()
        val pkgPath = packageName.replace('.', '/')

        writeRepositoryImplIfAllowed(subProjectRoot, template, pascal, pkgPath, moduleName)
        wireRootFeatureModule(subProjectRoot, packageName, pascal, pkgPath, moduleName)
        patchDataModuleRepositoryImport(subProjectRoot, packageName, pascal, pkgPath, moduleName)
        kmpFeatureBuildGradleUpdater.applyAfterApiSync(subProjectRoot, moduleName)
    }

    private fun writeRepositoryImplIfAllowed(
        subProjectRoot: File,
        template: ModuleTemplate,
        pascal: String,
        pkgPath: String,
        moduleName: String,
    ) {
        val packageName = template.packageName
        val file = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt",
        )
        val hasDb = generatedSupportExists(
            subProjectRoot,
            moduleName,
            packageName,
            "Generated${pascal}DbRepositorySupport.kt",
        )
        val hasPrefs = generatedSupportExists(
            subProjectRoot,
            moduleName,
            packageName,
            "Generated${pascal}PrefsRepositorySupport.kt",
        )
        val newContent = kmpRepositoryImplGenerator.renderDelegationRepositoryImpl(
            template = template,
            includeApi = true,
            includeDb = hasDb,
            includePrefs = hasPrefs,
        )
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
        if (text.contains("override suspend fun sample(")) return true
        if (text.contains("egs-codegen: scaffold-repository-impl-delegation")) return true
        if (text.contains("egs-codegen: scaffold-repository-impl")) return true
        if (text.contains("egs-codegen: db-only-repository-impl")) return true
        val legacy = "Generated${pascal}RepositorySupport"
        if (text.contains(legacy)) return true
        return false
    }

    private fun generatedSupportExists(
        subProjectRoot: File,
        moduleName: String,
        packageName: String,
        fileName: String,
    ): Boolean {
        val pkgPath = packageName.replace('.', '/')
        return subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/data/repository/$fileName",
        ).isFile
    }

    private fun wireRootFeatureModule(
        subProjectRoot: File,
        packageName: String,
        pascal: String,
        pkgPath: String,
        moduleName: String,
    ) {
        val diRoot = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/di/${pascal}Module.kt",
        )
        val legacyRoot = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/${pascal}Module.kt",
        )
        val rootFile = when {
            diRoot.exists() -> diRoot
            legacyRoot.exists() -> legacyRoot
            else -> {
                logger.debug("KMP feature root module not found (skip wire): {} or {}", diRoot.path, legacyRoot.path)
                return
            }
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
        val wiredListBlock = """
    generatedDataModule,
    dataModule,
    generatedDomainModule,
    domainModule,
    presentationModule,
""".trimEnd()
        var replaced = if (text.contains(defaultListBlock)) {
            text.replaceFirst(defaultListBlock, wiredListBlock)
        } else {
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

    /**
     * Point [DataModule] binding at [generate.domain.repository] after sync (replaces scaffold stub import).
     */
    private fun patchDataModuleRepositoryImport(
        subProjectRoot: File,
        packageName: String,
        pascal: String,
        pkgPath: String,
        moduleName: String,
    ) {
        val diFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/di/DataModule.kt",
        )
        val legacyFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/data/DataModule.kt",
        )
        val file = when {
            diFile.exists() -> diFile
            legacyFile.exists() -> legacyFile
            else -> return
        }

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

private fun String.kmpModulePascalCase(): String =
    split("-", "_").joinToString("") { part ->
        part.replaceFirstChar { c -> c.uppercase() }
    }.ifBlank { "Feature" }
