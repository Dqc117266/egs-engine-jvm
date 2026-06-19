/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Wires generated DI modules (`generatedDataModule`, `generatedDomainModule`) into the hand-written
 * aggregation entry point (`<Pascal>Module.kt` -> `feature<Pascal>Modules` list) after `gen database`.
 *
 * Also fixes [DataModule.kt] created by `module create`: its repository binding points at the legacy
 * `domain.repository.<Pascal>Repository`, but `gen database --repo` republishes the repository contract
 * under `generate.domain.repository.<Pascal>Repository`. We rewrite the binding + import so the
 * generated [Generated*DbRepositorySupport] dependency resolves and the generated DB access is exposed.
 *
 * Idempotent: safe to run multiple times and after `client api sync`.
 */
class KmpFeatureModuleAggregator {
    private val logger = LoggerFactory.getLogger(KmpFeatureModuleAggregator::class.java)

    fun apply(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
    ) {
        val pkg = template.packageName
        val pkgPath = pkg.replace('.', '/')
        val pascal = SqlNaming.moduleNameToPascal(moduleName)

        wireAggregationModule(subProjectRoot, moduleName, pkgPath, pkg, pascal)
        rewriteDataModuleRepositoryBinding(subProjectRoot, moduleName, pkgPath, pkg, pascal)
    }

    /**
     * Ensure `feature<Pascal>Modules` list contains `generatedDataModule` and `generatedDomainModule`,
     * with the matching imports from `<pkg>.generate.di`.
     */
    private fun wireAggregationModule(
        subProjectRoot: File,
        moduleName: String,
        pkgPath: String,
        pkg: String,
        pascal: String,
    ) {
        val file =
            subProjectRoot.resolve(
                "feature/$moduleName/src/commonMain/kotlin/$pkgPath/di/${pascal}Module.kt",
            )
        if (!file.exists()) {
            logger.debug("Aggregation module not found, skip: {}", file.path)
            return
        }

        val generatedDiPkg = "$pkg.generate.di"
        val generatedModules = listOf("generatedDataModule", "generatedDomainModule")

        var text = file.readText()
        val original = text

        // 1. Add imports for generated modules (idempotent).
        val importsToEnsure =
            generatedModules.map { "import $generatedDiPkg.$it" }
        text = mergeImports(text, importsToEnsure)

        // 2. Ensure each generated module name is present in the feature<Pascal>Modules list.
        val listNeedle = "feature${pascal}Modules: List<Module> = listOf("
        val listIdx = text.indexOf(listNeedle)
        if (listIdx >= 0) {
            val openParen = text.indexOf('(', listIdx)
            val closeParen = text.indexOf(')', openParen)
            if (openParen in (listIdx + 1) until closeParen) {
                val inner = text.substring(openParen + 1, closeParen)
                val entries =
                    inner
                        .split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toMutableList()
                for (m in generatedModules) {
                    if (entries.none { it == m || it.contains(".$m") }) {
                        entries += m
                    }
                }
                val newInner =
                    entries.joinToString(",\n    ") { it }
                text =
                    text.substring(0, openParen + 1) +
                        "\n    $newInner,\n" +
                        text.substring(closeParen)
            }
        }

        if (text != original) {
            file.writeText(text)
            logger.info("Wired generated modules into {}", file.path)
        }
    }

    /**
     * Rewrite the repository binding in [DataModule.kt] (created by `module create`) so it points at
     * `generate.domain.repository.<Pascal>Repository` instead of the legacy `domain.repository` one.
     * The generate contract extends the DB repository and is what [Generated*DbRepositorySupport]
     * satisfies, so binding to it exposes DB access through the hand-written RepositoryImpl.
     */
    private fun rewriteDataModuleRepositoryBinding(
        subProjectRoot: File,
        moduleName: String,
        pkgPath: String,
        pkg: String,
        pascal: String,
    ) {
        val file =
            subProjectRoot.resolve(
                "feature/$moduleName/src/commonMain/kotlin/$pkgPath/di/DataModule.kt",
            )
        if (!file.exists()) {
            logger.debug("DataModule not found, skip: {}", file.path)
            return
        }

        var text = file.readText()
        val original = text

        val repoName = "${pascal}Repository"
        val implName = "${pascal}RepositoryImpl"
        val newRepoImport = "import $pkg.generate.domain.repository.$repoName"
        val newImplImport = "import $pkg.data.repository.$implName"

        // Drop legacy `domain.repository.<Pascal>Repository` import if present (gen database
        // republishes the contract under `generate.domain.repository`).
        text = text.replace("import $pkg.domain.repository.$repoName\n", "")
        // Add imports for generate contract + impl (idempotent).
        text = mergeImports(text, listOf(newRepoImport, newImplImport))

        if (text != original) {
            file.writeText(text)
            logger.info("Rewrote repository binding in {}", file.path)
        }
    }

    private fun mergeImports(
        text: String,
        importsToEnsure: List<String>,
    ): String {
        val existingSet = text.lines().map { it.trim() }.toSet()
        val toAdd = importsToEnsure.filter { it.trim() !in existingSet }
        if (toAdd.isEmpty()) return text

        val lines = text.lines().toMutableList()
        val pkgIdx = lines.indexOfFirst { it.startsWith("package ") }
        if (pkgIdx < 0) return text.trimEnd() + "\n" + toAdd.joinToString("\n") + "\n"

        var lastImportIdx = pkgIdx
        for (j in pkgIdx + 1 until lines.size) {
            when {
                lines[j].startsWith("import ") -> lastImportIdx = j
                lines[j].isBlank() -> continue
                else -> break
            }
        }
        lines.addAll(lastImportIdx + 1, toAdd)
        return lines.joinToString("\n").trimEnd() + "\n"
    }
}
