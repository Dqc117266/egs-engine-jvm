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
 * Idempotently adds Koin binding for DB-only [RepositoryImpl] in [DataModule.kt] between
 * `// egs-gen:repository-begin` and `// egs-gen:repository-end`.
 */
class KmpDatabaseDbOnlyDataModuleUpdater {

    private val logger = LoggerFactory.getLogger(KmpDatabaseDbOnlyDataModuleUpdater::class.java)

    fun apply(subProjectRoot: File, moduleName: String, template: ModuleTemplate) {
        val pkg = template.packageName
        val pkgPath = pkg.replace('.', '/')
        val pascal = SqlNaming.moduleNameToPascal(moduleName)
        val repositoryName = "${pascal}Repository"
        val implName = "${pascal}RepositoryImpl"

        val diFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/di/DataModule.kt",
        )
        val legacyFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/data/DataModule.kt",
        )
        val file = when {
            diFile.exists() -> diFile
            legacyFile.exists() -> legacyFile
            else -> diFile
        }

        val body = """
    singleOf(::$implName) { bind<$repositoryName>() }
""".trimEnd()

        val importsToEnsure = listOf(
            "import org.koin.core.module.dsl.bind",
            "import org.koin.core.module.dsl.singleOf",
            "import $pkg.data.repository.$implName",
            "import $pkg.generate.domain.repository.$repositoryName",
        )

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(renderMinimalDataModule(pkg, importsToEnsure, body))
            logger.info("Created DataModule with DB-only repository binding at {}", file.path)
            return
        }

        var text = file.readText()
        if (text.contains("singleOf(::$implName)")) {
            logger.debug("Repository binding already present, skip: {}", file.path)
            return
        }
        if (!text.contains(BEGIN_MARKER)) {
            text = insertRepositoryMarkersIfMissing(text)
        }
        text = replaceRepositoryBlock(text, body)
        text = mergeImports(text, importsToEnsure)
        file.writeText(text)
        logger.info("Updated DataModule with DB-only repository binding: {}", file.path)
    }

    private fun renderMinimalDataModule(
        pkg: String,
        imports: List<String>,
        body: String,
    ): String = buildString {
        appendLine("package $pkg.di")
        appendLine()
        imports.forEach { appendLine(it) }
        appendLine("import org.koin.dsl.module")
        appendLine()
        appendLine("internal val dataModule = module {")
        appendLine("    // egs-gen:repository-begin")
        appendLine(body)
        appendLine("    // egs-gen:repository-end")
        appendLine("}")
        appendLine()
    }.toString()

    private fun insertRepositoryMarkersIfMissing(text: String): String {
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
                        if (inner.contains("egs-gen:repository")) return text
                        return text.substring(0, i) +
                            "\n\n    // egs-gen:repository-begin\n\n    // egs-gen:repository-end\n" +
                            text.substring(i)
                    }
                }
            }
            i++
        }
        return text
    }

    private fun replaceRepositoryBlock(text: String, body: String): String {
        val pattern = Regex(
            """[ \t]*// egs-gen:repository-begin\s*\n([\s\S]*?)\n[ \t]*// egs-gen:repository-end""",
            RegexOption.MULTILINE,
        )
        return pattern.replace(text) {
            "// egs-gen:repository-begin\n$body\n    // egs-gen:repository-end"
        }
    }

    private fun mergeImports(text: String, importsToEnsure: List<String>): String {
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

    private companion object {
        const val BEGIN_MARKER = "// egs-gen:repository-begin"
    }
}
