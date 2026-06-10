/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.DatabaseEntityDomainMapping
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Mode A: generates DB slice [TodoDbRepository] interface and [GeneratedTodoDbRepositorySupport].
 */
class KmpDatabaseRepositoryGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(KmpDatabaseRepositoryGenerator::class.java)

    fun generateDbOnlyRepository(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        projectRoot: File?,
        spec: SwaggerSpec? = null,
    ): List<GeneratedFile> {
        require(tables.isNotEmpty()) { "At least one table is required for DB-only repository" }

        val pkg = template.packageName
        val databasePackageName = "$pkg.generate.data.datasource.database"
        val entityPackageName = "$databasePackageName.entity"
        val domainPackageName = "$pkg.generate.domain.model"
        val mapperPackageName = "$databasePackageName.mapper"
        val modulePascal = SqlNaming.moduleNameToPascal(template.name)
        val moduleDatabaseName = "${modulePascal}Database"
        val dbRepositoryName = "${modulePascal}DbRepository"
        val dbRepositorySupportName = "Generated${modulePascal}DbRepositorySupport"
        val repositoryPackageName = "$pkg.generate.domain.repository"
        val repositorySupportPackageName = "$pkg.generate.data.repository"

        val moduleDir = "feature/${template.name}"
        val rows = KmpDatabaseTemplateModels.buildRows(tables)

        val tableMaps =
            rows.map { row ->
                val domainSimple = DatabaseEntityDomainMapping.resolveDomainClassName(row.table, spec)
                val hasDomainMapping = domainSimple != null
                val exposedRowType = domainSimple ?: row.entityClassName
                mapOf(
                    "sqlTableName" to row.table.tableName,
                    "entityClassName" to row.entityClassName,
                    "exposedRowType" to exposedRowType,
                    "hasDomainMapping" to hasDomainMapping,
                    "prefixPascal" to row.prefixPascal,
                    "pkPropertyName" to row.pkPropertyName,
                    "pkKotlinType" to row.pkKotlinType,
                )
            }

        val rowImports =
            rows
                .map { row ->
                    val domainSimple = DatabaseEntityDomainMapping.resolveDomainClassName(row.table, spec)
                    if (domainSimple != null) {
                        "$domainPackageName.$domainSimple"
                    } else {
                        "$entityPackageName.${row.entityClassName}"
                    }
                }.distinct()
                .sorted()

        val hasAnyDomainMapping = tableMaps.any { it["hasDomainMapping"] == true }

        val repoInterface =
            templateEngine.render(
                "kmp/database/DbRepository.kt.ftl",
                mapOf(
                    "repositoryPackageName" to repositoryPackageName,
                    "dbRepositoryName" to dbRepositoryName,
                    "moduleDatabaseName" to moduleDatabaseName,
                    "rowImports" to rowImports,
                    "tables" to tableMaps,
                ),
                projectRoot,
            )

        val repoSupport =
            templateEngine.render(
                "kmp/database/DbRepositorySupport.kt.ftl",
                mapOf(
                    "repositorySupportPackageName" to repositorySupportPackageName,
                    "repositoryPackageName" to repositoryPackageName,
                    "dbRepositoryName" to dbRepositoryName,
                    "dbRepositorySupportName" to dbRepositorySupportName,
                    "databasePackageName" to databasePackageName,
                    "moduleDatabaseName" to moduleDatabaseName,
                    "mapperPackageName" to mapperPackageName,
                    "hasAnyDomainMapping" to hasAnyDomainMapping,
                    "rowImports" to rowImports,
                    "tables" to tableMaps,
                ),
                projectRoot,
            )

        val files = mutableListOf<GeneratedFile>()
        files.add(generatedCommonMain(moduleDir, repositoryPackageName, "$dbRepositoryName.kt", repoInterface))
        files.add(generatedCommonMain(moduleDir, repositorySupportPackageName, "$dbRepositorySupportName.kt", repoSupport))

        logger.info("Generated DB repository slice ({} files) for module {}", files.size, template.name)
        return files
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
