/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Fills or creates [GeneratedDataModule.kt] between `// egs-gen:database-begin` and `// egs-gen:database-end`.
 */
class KmpDatabaseGeneratedDataModuleUpdater {

    private val logger = LoggerFactory.getLogger(KmpDatabaseGeneratedDataModuleUpdater::class.java)

    /**
     * @param includeDbRepositorySupport Register [Generated*DbRepositorySupport] in Koin only when
     * `gen database --repo` actually emits that class (omit when generating Room/DAO only, or when `--cached` runs without repo slice).
     */
    fun apply(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
        tables: List<TableSchema>,
        includeDbRepositorySupport: Boolean = true,
    ) {
        val pkg = template.packageName
        val pkgPath = pkg.replace('.', '/')
        val generateDiPackage = "$pkg.generate.di"
        val file = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/di/GeneratedDataModule.kt",
        )

        val modulePascal = SqlNaming.moduleNameToPascal(moduleName)
        val moduleDatabaseName = "${modulePascal}Database"
        val dataSourceClassName = "${moduleDatabaseName}DataSource"
        val databasePkg = "$pkg.generate.data.datasource.database"
        val daoPkg = "$databasePkg.dao"

        val tableModels: List<Pair<String, String>> = tables.map { table ->
            val base = SqlNaming.snakeToPascal(table.tableName)
            val daoClassName = "${base}Dao"
            val daoPropertyName = daoClassName.replaceFirstChar { it.lowercase() }
            daoClassName to daoPropertyName
        }

        val dbRepositorySupportName = "Generated${modulePascal}DbRepositorySupport"

        val body = buildDatabaseBody(
            moduleDatabaseName,
            dataSourceClassName,
            tableModels,
            dbRepositorySupportName,
            includeDbRepositorySupport,
        )
        val repositoryPkg = "$pkg.generate.data.repository"
        val importsToEnsure = buildList {
            add("import org.koin.core.module.dsl.singleOf")
            add("import $CORE_BASE_DB_PKG.AppRoomDatabase")
            add("import $CORE_BASE_DB_PKG.DatabaseBuilderFactory")
            add("import $CORE_BASE_DB_PKG.create")
            add("import $databasePkg.$moduleDatabaseName")
            add("import $databasePkg.$dataSourceClassName")
            if (includeDbRepositorySupport) {
                add("import $repositoryPkg.$dbRepositorySupportName")
            }
            for ((daoClassName, _) in tableModels) {
                add("import $daoPkg.$daoClassName")
            }
        }

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(renderStandaloneModule(generateDiPackage, importsToEnsure, body))
            logger.info("Created GeneratedDataModule with database bindings at {}", file.path)
            return
        }

        var text = file.readText()
        if (!text.contains(BEGIN_MARKER)) {
            text = insertMarkersIfMissing(text)
        }
        text = replaceDatabaseBlock(text, body)
        text = mergeImports(text, importsToEnsure)
        if (!includeDbRepositorySupport) {
            text = removeDbRepositorySupportImport(text, dbRepositorySupportName)
        }
        file.writeText(text)
        logger.info("Updated database bindings in {}", file.path)
    }

    private fun buildDatabaseBody(
        moduleDatabaseName: String,
        dataSourceClassName: String,
        tableModels: List<Pair<String, String>>,
        dbRepositorySupportName: String,
        includeDbRepositorySupport: Boolean,
    ): String = buildString {
        appendLine("    single<$moduleDatabaseName> {")
        appendLine("        get<DatabaseBuilderFactory>().create<$moduleDatabaseName>(AppRoomDatabase.FILE_NAME)")
        appendLine("    }")
        for ((_, daoProp) in tableModels) {
            appendLine("    single { get<$moduleDatabaseName>().$daoProp() }")
        }
        appendLine("    singleOf(::$dataSourceClassName)")
        if (includeDbRepositorySupport) {
            appendLine("    singleOf(::$dbRepositorySupportName)")
        }
    }.trimEnd()

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
        body.lines().forEach { appendLine(it) }
        appendLine("    // egs-gen:database-end")
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
                        if (inner.contains("egs-gen:database")) return text
                        return text.substring(0, i) +
                            "\n\n    // egs-gen:database-begin\n\n    // egs-gen:database-end\n" +
                            text.substring(i)
                    }
                }
            }
            i++
        }
        return text
    }

    private fun replaceDatabaseBlock(text: String, body: String): String {
        val pattern = Regex(
            """[ \t]*// egs-gen:database-begin\s*\n([\s\S]*?)\n[ \t]*// egs-gen:database-end""",
            RegexOption.MULTILINE,
        )
        return pattern.replace(text) {
            "// egs-gen:database-begin\n$body\n    // egs-gen:database-end"
        }
    }

    private fun removeDbRepositorySupportImport(text: String, simpleClassName: String): String {
        val lines = text.lines().filterNot { line ->
            val t = line.trim()
            t.startsWith("import ") && t.endsWith(simpleClassName)
        }
        return lines.joinToString("\n").trimEnd() + "\n"
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
        const val CORE_BASE_DB_PKG = "template.core.base.database"
        const val BEGIN_MARKER = "// egs-gen:database-begin"
    }
}
