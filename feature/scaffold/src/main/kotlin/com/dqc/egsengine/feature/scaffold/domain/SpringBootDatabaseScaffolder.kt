/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootCrudGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootGeneratedPathsManifest
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootHandWrittenShellGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import org.slf4j.LoggerFactory
import java.io.File

/**
 * DDL-driven scaffold for Spring Boot generated sources + handwritten shells + `.egs-generated.json`.
 */
class SpringBootDatabaseScaffolder(
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val ddlParser: DdlParser,
    private val crudGenerator: SpringBootCrudGenerator,
    private val moduleScaffolder: ModuleScaffolder,
    private val manifest: SpringBootGeneratedPathsManifest,
    private val shells: SpringBootHandWrittenShellGenerator,
) {
    private val logger = LoggerFactory.getLogger(SpringBootDatabaseScaffolder::class.java)

    data class Result(
        val moduleName: String,
        val tableName: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )

    fun scaffoldDatabase(
        projectRoot: File,
        sqlFile: File,
        moduleName: String,
        dryRun: Boolean = false,
        force: Boolean = false,
        mainTable: String? = null,
        options: SpringBootOpinionatedOptions = SpringBootOpinionatedOptions(),
    ): Result {
        val backendCfg = workspaceConfigResolver.resolveBackend(projectRoot)
        require(backendCfg.platform == Platform.SPRING_BOOT) {
            "backend gen database requires workspace project 'backend' with platform spring_boot; got ${backendCfg.platform}"
        }

        val backendRoot = projectRoot.resolve(backendCfg.path).normalize()
        val moduleRoot = backendRoot.resolve("feature/$moduleName")

        if (!moduleRoot.exists() && !dryRun) {
            moduleScaffolder.scaffoldForProject(
                projectRoot = projectRoot,
                moduleName = moduleName,
                projectKey = "backend",
                dryRun = false,
            )
            logger.info("Created missing backend module 'feature:{}' before codegen", moduleName)
        }

        val tables = ddlParser.parseFile(sqlFile)
        require(tables.isNotEmpty()) { "No CREATE TABLE statements in ${sqlFile.path}" }

        val table =
            when {
                mainTable != null ->
                    tables.singleOrNull { it.tableName.equals(mainTable, ignoreCase = true) }
                        ?: error(
                            "Could not find table '$mainTable' in ${sqlFile.path}. Found: ${tables.map { it.tableName }}",
                        )
                tables.size == 1 -> tables.first()
                else -> error(
                    "DDL defines ${tables.size} tables; pass --main-table=<name>. Tables: ${tables.map { it.tableName }}",
                )
            }

        val entityPascal = SqlNaming.tableToEntityPascal(table.tableName)
        val entityCamel = entityPascal.replaceFirstChar { it.lowercase() }

        if (!dryRun && force) {
            manifest.read(backendRoot, moduleName)?.let { dto ->
                manifest.deleteTrackedFiles(backendRoot, dto)
            }
        }

        val codegenFiles = crudGenerator.generate(table, moduleName, backendCfg, options, projectRoot)

        val shellFiles =
            shells.generateIfMissing(
                backendRoot = backendRoot,
                moduleName = moduleName,
                config = backendCfg,
                entityPascal = entityPascal,
                entityCamel = entityCamel,
                force = force,
                dryRun = dryRun,
            )

        val all = codegenFiles + shellFiles

        if (!dryRun) {
            for (file in all) {
                val target = backendRoot.resolve(file.path)
                target.parentFile?.mkdirs()
                file.content?.let { target.writeText(it) }
            }
            manifest.write(backendRoot, moduleName, table.tableName, all.map { it.path })
            logger.info(
                "SpringBoot database scaffold: table '{}' -> module '{}' ({} files)",
                table.tableName,
                moduleName,
                all.size,
            )
        }

        return Result(
            moduleName = moduleName,
            tableName = table.tableName,
            files = all,
            dryRun = dryRun,
        )
    }
}
