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
 * Generates Room Entity / DAO / single [Module]Database / DataSource under
 * `feature/<module>/src/commonMain/kotlin/.../generate/data/datasource/database/`.
 */
class KmpDatabaseCodeGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(KmpDatabaseCodeGenerator::class.java)

    fun generate(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        projectRoot: File? = null,
    ): List<GeneratedFile> {
        require(tables.isNotEmpty()) { "At least one CREATE TABLE is required" }

        val pkg = template.packageName
        val databasePackageName = "$pkg.generate.data.datasource.database"
        val entityPackageName = "$databasePackageName.entity"
        val daoPackageName = "$databasePackageName.dao"
        val modulePascal = SqlNaming.moduleNameToPascal(template.name)
        val moduleDatabaseName = "${modulePascal}Database"

        val moduleDir = "feature/${template.name}"
        val files = mutableListOf<GeneratedFile>()

        val tableModels = KmpDatabaseTemplateModels.buildRows(tables)

        for (t in tableModels) {
            val entityContent = templateEngine.render(
                "kmp/database/Entity.kt.ftl",
                mapOf(
                    "entityPackageName" to entityPackageName,
                    "entityClassName" to t.entityClassName,
                    "table" to mapOf("tableName" to t.table.tableName),
                    "columns" to t.entityColumns,
                ),
                projectRoot,
            )
            files.add(
                generatedCommonMain(
                    moduleDir,
                    entityPackageName,
                    "${t.entityClassName}.kt",
                    entityContent,
                ),
            )

            val daoContent = templateEngine.render(
                "kmp/database/Dao.kt.ftl",
                mapOf(
                    "daoPackageName" to daoPackageName,
                    "entityPackageName" to entityPackageName,
                    "entityClassName" to t.entityClassName,
                    "daoClassName" to t.daoClassName,
                    "table" to mapOf("tableName" to t.table.tableName),
                    "orderByColumn" to t.orderByColumnName,
                    "pkColumnName" to t.pkColumnName,
                    "pkPropertyName" to t.pkPropertyName,
                    "pkKotlinType" to t.pkKotlinType,
                ),
                projectRoot,
            )
            files.add(
                generatedCommonMain(
                    moduleDir,
                    daoPackageName,
                    "${t.daoClassName}.kt",
                    daoContent,
                ),
            )
        }

        val dbContent = templateEngine.render(
            "kmp/database/Database.kt.ftl",
            mapOf(
                "databasePackageName" to databasePackageName,
                "moduleDatabaseName" to moduleDatabaseName,
                "tables" to tableModels.map { tm ->
                    mapOf(
                        "entityPackageName" to entityPackageName,
                        "daoPackageName" to daoPackageName,
                        "entityClassName" to tm.entityClassName,
                        "daoClassName" to tm.daoClassName,
                        "daoPropertyName" to tm.daoPropertyName,
                    )
                },
            ),
            projectRoot,
        )
        files.add(
            generatedCommonMain(
                moduleDir,
                databasePackageName,
                "$moduleDatabaseName.kt",
                dbContent,
            ),
        )

        val dsContent = templateEngine.render(
                "kmp/database/DatabaseDataSource.kt.ftl",
            mapOf(
                "databasePackageName" to databasePackageName,
                "moduleDatabaseName" to moduleDatabaseName,
                "tables" to tableModels.map { tm ->
                    mapOf(
                        "entityPackageName" to entityPackageName,
                        "entityClassName" to tm.entityClassName,
                        "daoPackageName" to daoPackageName,
                        "daoClassName" to tm.daoClassName,
                        "daoPropertyName" to tm.daoPropertyName,
                        "prefixPascal" to tm.prefixPascal,
                        "pkPropertyName" to tm.pkPropertyName,
                        "pkKotlinType" to tm.pkKotlinType,
                    )
                },
            ),
            projectRoot,
        )
        files.add(
            generatedCommonMain(
                moduleDir,
                databasePackageName,
                "${moduleDatabaseName}DataSource.kt",
                dsContent,
            ),
        )

        logger.info("Generated {} KMP database files for module {}", files.size, template.name)
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
