package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import java.io.File

/**
 * Workspace Spring Boot backend DDL → CRUD entry point (CLI also uses [SpringBootDatabaseScaffolder] directly).
 */
class EntityScaffolder(
    private val databaseScaffolder: SpringBootDatabaseScaffolder,
) {
    fun scaffold(
        projectRoot: File,
        moduleName: String,
        ddlSql: String,
        ddlFileName: String = "ddl.sql",
        dryRun: Boolean = false,
        options: SpringBootOpinionatedOptions = SpringBootOpinionatedOptions(),
    ): EntityScaffoldResult {
        val tmp = File.createTempFile("egs-ddl-", ddlFileName)
        try {
            tmp.writeText(ddlSql)
            val r =
                databaseScaffolder.scaffoldDatabase(
                    projectRoot = projectRoot,
                    sqlFile = tmp,
                    moduleName = moduleName,
                    dryRun = dryRun,
                    options = options,
                )
            return EntityScaffoldResult(
                moduleName = r.moduleName,
                tables = listOf(r.tableName),
                files = r.files,
                dryRun = r.dryRun,
            )
        } finally {
            tmp.delete()
        }
    }

    fun scaffoldFromFile(
        projectRoot: File,
        moduleName: String,
        ddlFile: File,
        dryRun: Boolean = false,
        options: SpringBootOpinionatedOptions = SpringBootOpinionatedOptions(),
    ): EntityScaffoldResult {
        val r =
            databaseScaffolder.scaffoldDatabase(
                projectRoot = projectRoot,
                sqlFile = ddlFile,
                moduleName = moduleName,
                dryRun = dryRun,
                options = options,
            )
        return EntityScaffoldResult(
            moduleName = r.moduleName,
            tables = listOf(r.tableName),
            files = r.files,
            dryRun = r.dryRun,
        )
    }

    data class EntityScaffoldResult(
        val moduleName: String,
        val tables: List<String>,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
