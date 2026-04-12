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
 * Mode B: injects [ModuleDatabaseDataSource] into existing Swagger-generated
 * [Generated*RepositorySupport] and [ *RepositoryImpl ] when `gen database --repo` runs after api sync.
 */
class KmpDatabaseRepositoryUpdater {

    private val logger = LoggerFactory.getLogger(KmpDatabaseRepositoryUpdater::class.java)

    fun apply(subProjectRoot: File, moduleName: String, template: ModuleTemplate) {
        val pkg = template.packageName
        val pkgPath = pkg.replace('.', '/')
        val pascal = SqlNaming.moduleNameToPascal(moduleName)
        val moduleDatabaseName = "${pascal}Database"
        val dataSourceClass = "${moduleDatabaseName}DataSource"
        val dataSourceImport = "$pkg.generate.data.datasource.database.$dataSourceClass"

        val supportFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/data/repository/Generated${pascal}RepositorySupport.kt",
        )
        if (!supportFile.exists()) {
            logger.debug("No Swagger GeneratedRepositorySupport at {}, skip Mode B patch", supportFile.path)
            return
        }

        var supportText = supportFile.readText()
        if (supportText.contains("dbDataSource: $dataSourceClass") ||
            supportText.contains("val dbDataSource: $dataSourceClass")
        ) {
            logger.debug("GeneratedRepositorySupport already has dbDataSource, skip")
            return
        }

        supportText = ensureImport(supportText, "import $dataSourceImport")
        supportText = patchGeneratedRepositorySupportConstructor(supportText, pascal, dataSourceClass)
        supportFile.writeText(supportText)
        logger.info("Patched Generated{}RepositorySupport with {}", pascal, dataSourceClass)

        val implFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt",
        )
        if (!implFile.exists()) {
            logger.debug("No RepositoryImpl at {}, skip impl patch", implFile.path)
            return
        }
        var implText = implFile.readText()
        if (implText.contains(FREEZE_MARKER)) {
            logger.debug("RepositoryImpl frozen, skip: {}", implFile.path)
            return
        }
        if (implText.contains("dbDataSource: $dataSourceClass")) {
            return
        }
        implText = ensureImport(implText, "import $dataSourceImport")
        implText = patchRepositoryImplConstructor(implText, pascal, dataSourceClass)
        implFile.writeText(implText)
        logger.info("Patched {}RepositoryImpl with {}", pascal, dataSourceClass)
    }

    private fun patchGeneratedRepositorySupportConstructor(
        text: String,
        pascal: String,
        dataSourceClass: String,
    ): String {
        val pattern = Regex(
            """(private\s+val\s+service:\s+${Regex.escape(pascal)}KtorfitService\s*,)\s*\)""",
            RegexOption.MULTILINE,
        )
        val replaced = pattern.replace(text) { m ->
            val serviceLine = m.groupValues[1]
            "$serviceLine\n    protected val dbDataSource: $dataSourceClass,\n)"
        }
        if (replaced == text) {
            logger.warn("Could not patch Generated{}RepositorySupport constructor - apply dbDataSource manually.", pascal)
        }
        return replaced
    }

    private fun patchRepositoryImplConstructor(
        text: String,
        pascal: String,
        dataSourceClass: String,
    ): String {
        val pattern = Regex(
            """(internal\s+class\s+${Regex.escape(pascal)}RepositoryImpl\s*\(\s*)""" +
                """(service:\s+${Regex.escape(pascal)}KtorfitService\s*,)\s*\)\s*:\s*Generated${Regex.escape(pascal)}RepositorySupport\s*\(\s*service\s*\)""",
            RegexOption.MULTILINE,
        )
        val replaced = pattern.replace(text) { m ->
            val head = m.groupValues[1]
            val serviceLine = m.groupValues[2]
            "$head$serviceLine\n    dbDataSource: $dataSourceClass,\n) : Generated${pascal}RepositorySupport(service, dbDataSource)"
        }
        if (replaced == text) {
            // Try single-line variant without trailing comma on service
            val pattern2 = Regex(
                """(internal\s+class\s+${Regex.escape(pascal)}RepositoryImpl\s*\(\s*service:\s+${Regex.escape(pascal)}KtorfitService\s*\)\s*:\s*Generated${Regex.escape(pascal)}RepositorySupport\s*\(\s*service\s*\))""",
                RegexOption.MULTILINE,
            )
            return pattern2.replace(text) {
                "internal class ${pascal}RepositoryImpl(\n    service: ${pascal}KtorfitService,\n    dbDataSource: $dataSourceClass,\n) : Generated${pascal}RepositorySupport(service, dbDataSource)"
            }
        }
        return replaced
    }

    private fun ensureImport(text: String, importLine: String): String {
        if (text.lines().any { it.trim() == importLine.trim() }) return text
        val lines = text.lines().toMutableList()
        val pkgIdx = lines.indexOfFirst { it.startsWith("package ") }
        if (pkgIdx < 0) return "$importLine\n\n$text"
        var lastImport = pkgIdx
        for (j in pkgIdx + 1 until lines.size) {
            when {
                lines[j].startsWith("import ") -> lastImport = j
                lines[j].isBlank() -> continue
                else -> break
            }
        }
        lines.add(lastImport + 1, importLine)
        return lines.joinToString("\n").trimEnd() + "\n"
    }

    private companion object {
        const val FREEZE_MARKER = "// egs-sync:freeze"
    }
}
