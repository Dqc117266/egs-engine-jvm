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
 * Merges `// egs-gen:prefs-begin` through `// egs-gen:prefs-end` inside [GeneratedDataModule.kt].
 */
class KmpPrefsGeneratedDataModuleUpdater {

    private val logger = LoggerFactory.getLogger(KmpPrefsGeneratedDataModuleUpdater::class.java)

    fun apply(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
    ) {
        val pkg = template.packageName
        val pkgPath = pkg.replace('.', '/')
        val generateDiPackage = "$pkg.generate.di"
        val file = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/di/GeneratedDataModule.kt",
        )

        val modulePascal = SqlNaming.moduleNameToPascal(moduleName)
        val prefsDataSource = "${modulePascal}PreferencesDataSource"
        val prefsSupport = "Generated${modulePascal}PrefsRepositorySupport"
        val prefsDsPkg = "$pkg.generate.data.datasource.preferences"
        val prefsRepoPkg = "$pkg.generate.data.repository"

        val body = """
    singleOf(::$prefsDataSource)
    singleOf(::$prefsSupport)
""".trimEnd()

        val importsToEnsure = listOf(
            "import org.koin.core.module.dsl.singleOf",
            "import $prefsDsPkg.$prefsDataSource",
            "import $prefsRepoPkg.$prefsSupport",
        )

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(renderStandaloneModule(generateDiPackage, importsToEnsure, body))
            logger.info("Created GeneratedDataModule with prefs bindings at {}", file.path)
            return
        }

        var text = file.readText()
        if (!text.contains(BEGIN_MARKER)) {
            text = insertMarkersIfMissing(text)
        }
        text = replacePrefsBlock(text, body)
        text = mergeImports(text, importsToEnsure)
        file.writeText(text)
        logger.info("Updated prefs bindings in {}", file.path)
    }

    private fun renderStandaloneModule(
        generateDiPackage: String,
        imports: List<String>,
        body: String,
    ): String = buildString {
        appendLine("package $generateDiPackage")
        appendLine()
        imports.forEach { appendLine(it) }
        appendLine("import org.koin.dsl.module")
        appendLine()
        appendLine("internal val generatedDataModule = module {")
        appendLine("    // egs-gen:database-begin")
        appendLine()
        appendLine("    // egs-gen:database-end")
        appendLine()
        appendLine("    // egs-gen:prefs-begin")
        body.lines().forEach { appendLine(it) }
        appendLine("    // egs-gen:prefs-end")
        appendLine("}")
        appendLine()
    }.toString()

    private fun insertMarkersIfMissing(text: String): String {
        val needle = "= module {"
        val moduleIdx = text.indexOf(needle)
        if (moduleIdx < 0) return text
        val openIdx = text.indexOf('{', moduleIdx)
        if (openIdx < 0) return text
        var depth = 0
        var i = openIdx
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val inner = text.substring(openIdx + 1, i)
                        if (inner.contains("egs-gen:prefs")) return text
                        return text.substring(0, i) +
                            "\n\n    // egs-gen:prefs-begin\n\n    // egs-gen:prefs-end\n" +
                            text.substring(i)
                    }
                }
            }
            i++
        }
        return text
    }

    private fun replacePrefsBlock(text: String, body: String): String {
        val pattern = Regex(
            """[ \t]*// egs-gen:prefs-begin\s*\n([\s\S]*?)\n[ \t]*// egs-gen:prefs-end""",
            RegexOption.MULTILINE,
        )
        return pattern.replace(text) {
            "    // egs-gen:prefs-begin\n$body\n    // egs-gen:prefs-end"
        }
    }

    private fun mergeImports(text: String, importsToEnsure: List<String>): String {
        val existingImports = text.lines()
            .filter { it.trim().startsWith("import ") }
            .map { it.trim() }
            .toSet()
        val toAdd = importsToEnsure.filter { it.trim() !in existingImports }
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

    private companion object {
        const val BEGIN_MARKER = "// egs-gen:prefs-begin"
    }
}
