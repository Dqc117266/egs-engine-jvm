/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Mode A: generates DB-only [Repository] interface, [Generated*RepositorySupport], and handwritten [RepositoryImpl] scaffold.
 */
class KmpDatabaseRepositoryGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(KmpDatabaseRepositoryGenerator::class.java)

    fun generateDbOnlyRepository(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        projectRoot: File?,
    ): List<GeneratedFile> {
        require(tables.isNotEmpty()) { "At least one table is required for DB-only repository" }

        val pkg = template.packageName
        val databasePackageName = "$pkg.generate.data.datasource.database"
        val entityPackageName = "$databasePackageName.entity"
        val modulePascal = SqlNaming.moduleNameToPascal(template.name)
        val moduleDatabaseName = "${modulePascal}Database"
        val repositoryName = "${modulePascal}Repository"
        val repositorySupportName = "Generated${modulePascal}RepositorySupport"
        val repositoryPackageName = "$pkg.generate.domain.repository"
        val repositorySupportPackageName = "$pkg.generate.data.repository"

        val moduleDir = "feature/${template.name}"
        val rows = KmpDatabaseTemplateModels.buildRows(tables)

        val entityImports = rows.map { "$entityPackageName.${it.entityClassName}" }.sorted().distinct()

        val tableMaps = rows.map { row ->
            mapOf(
                "sqlTableName" to row.table.tableName,
                "entityClassName" to row.entityClassName,
                "prefixPascal" to row.prefixPascal,
                "pkPropertyName" to row.pkPropertyName,
                "pkKotlinType" to row.pkKotlinType,
            )
        }

        val repoInterface = templateEngine.render(
            "kmp/database/Repository.kt.ftl",
            mapOf(
                "repositoryPackageName" to repositoryPackageName,
                "repositoryName" to repositoryName,
                "moduleDatabaseName" to moduleDatabaseName,
                "entityImports" to entityImports,
                "tables" to tableMaps,
            ),
            projectRoot,
        )

        val repoSupport = templateEngine.render(
            "kmp/database/GeneratedRepositorySupport.kt.ftl",
            mapOf(
                "repositorySupportPackageName" to repositorySupportPackageName,
                "repositoryPackageName" to repositoryPackageName,
                "repositoryName" to repositoryName,
                "repositorySupportName" to repositorySupportName,
                "databasePackageName" to databasePackageName,
                "moduleDatabaseName" to moduleDatabaseName,
                "entityImports" to entityImports,
                "tables" to tableMaps,
            ),
            projectRoot,
        )

        val impl = renderDbOnlyRepositoryImpl(
            packageName = pkg,
            modulePascal = modulePascal,
            moduleDatabaseName = moduleDatabaseName,
            repositorySupportName = repositorySupportName,
        )

        val files = mutableListOf<GeneratedFile>()
        files.add(
            generatedCommonMain(moduleDir, repositoryPackageName, "$repositoryName.kt", repoInterface),
        )
        files.add(
            generatedCommonMain(moduleDir, repositorySupportPackageName, "$repositorySupportName.kt", repoSupport),
        )
        files.add(
            generatedCommonMain(moduleDir, "$pkg.data.repository", "${modulePascal}RepositoryImpl.kt", impl),
        )

        logger.info("Generated DB-only repository ({} files) for module {}", files.size, template.name)
        return files
    }

    private fun renderDbOnlyRepositoryImpl(
        packageName: String,
        modulePascal: String,
        moduleDatabaseName: String,
        repositorySupportName: String,
    ): String {
        val dataSourceClass = "${moduleDatabaseName}DataSource"
        return """
        /*
         * Hand-written repository: extends generated DB-only support.
         * egs-codegen: db-only-repository-impl
         * Add // egs-sync:freeze on its own line to prevent overwrites.
         */
        package $packageName.data.repository

        import $packageName.generate.data.datasource.database.$dataSourceClass
        import $packageName.generate.data.repository.$repositorySupportName

        internal class ${modulePascal}RepositoryImpl(
            dbDataSource: $dataSourceClass,
        ) : $repositorySupportName(dbDataSource) {
        }
        """.trimIndent() + "\n"
    }

    private fun generatedCommonMain(
        moduleDir: String,
        packageName: String,
        fileName: String,
        content: String,
    ): GeneratedFile {
        val pkgPath = packageName.replace('.', '/')
        return GeneratedFile(
            "$moduleDir/src/commonMain/kotlin/$pkgPath/$fileName",
            content,
        )
    }
}
