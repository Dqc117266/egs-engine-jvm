/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.BackendCodegenManifest
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.BackendCodegenManifestColumn
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.FormControl

/**
 * Emits Vue3 admin CRUD files from [BackendCodegenManifest] (written by backend `gen database`).
 *
 * Sidebar routes are driven by Postgres sys_menus; the browser does not use hand-written vue-router modules as the authority.
 * This scaffolder therefore also writes a Flyway migration under `backend/app/.../db/migration/` (see [writeSysMenuFlyway])
 * so that starting the Spring Boot app runs Flyway and persists menu rows — that is intentional and part of the admin codegen contract.
 */
class AdminVueCrudScaffolder(
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(AdminVueCrudScaffolder::class.java)

    data class Result(
        val moduleName: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
        val sysMenuFlywayMigration: GeneratedFile? = null,
    )

    fun scaffoldFromCodegen(
        projectRoot: File,
        codegen: BackendCodegenManifest,
        dryRun: Boolean,
        ddlSql: String? = null,
    ): Result {
        val adminCfg = workspaceConfigResolver.resolveAdmin(projectRoot)
        require(adminCfg.platform == Platform.VUE3) {
            "admin gen from-backend requires workspace project 'admin' with platform vue3; got ${adminCfg.platform}"
        }
        val adminRoot = projectRoot.resolve(adminCfg.path).normalize()
        val backendCfg = workspaceConfigResolver.resolveBackend(projectRoot)
        val backendRoot = projectRoot.resolve(backendCfg.path).normalize()
        val module = codegen.backendModuleName
        val model = buildFreemarkerModel(codegen, ddlSql)

        val files = mutableListOf<GeneratedFile>()
        files += renderPair("vue3/admin/api_ts.ftl", "src/api/$module.ts", model)
        files += renderPair("vue3/admin/types_ts.ftl", "src/types/$module.ts", model)
        files += renderPair("vue3/admin/store_ts.ftl", "src/stores/$module.ts", model)
        files += renderPair("vue3/admin/index_vue.ftl", "src/views/$module/index.vue", model)
        files += renderPair("vue3/admin/router_ts.ftl", "src/router/modules/$module.ts", model)
        files += renderPair("vue3/admin/menu_sql.ftl", "src/sql/sys_menu_snippet_$module.sql", model)

        if (!dryRun) {
            for (f in files) {
                val target = adminRoot.resolve(f.path)
                target.parentFile?.mkdirs()
                f.content?.let { target.writeText(it) }
            }
            logger.info("Admin Vue scaffold: module '{}' ({} files)", module, files.size)
        }

        val flywayMigration = writeSysMenuFlyway(backendRoot, codegen, model, dryRun, ddlSql)

        return Result(moduleName = module, files = files, dryRun = dryRun, sysMenuFlywayMigration = flywayMigration)
    }

    private fun writeSysMenuFlyway(
        backendRoot: File,
        codegen: BackendCodegenManifest,
        model: Map<String, Any?>,
        dryRun: Boolean,
        ddlSql: String? = null,
    ): GeneratedFile? {
        val migrationDir = backendRoot.resolve("app/src/main/resources/db/migration").normalize()
        val moduleSlug = codegen.backendModuleName
        val sqlName = resolveSysMenuMigrationFileName(migrationDir, moduleSlug)

        val relativeRef = "app/src/main/resources/db/migration/$sqlName"

        val content = templateEngine.render("springboot/sys_menu_flyway.ftl", model, null)
        val gf = GeneratedFile(path = relativeRef, content = content)

        if (!dryRun) {
            migrationDir.mkdirs()
            val targetFile = migrationDir.resolve(sqlName)
            targetFile.writeText(content.trimEnd() + "\n")
            logger.info(
                "Sys menu Flyway migration written: {} (same filename reused per module when re-running codegen.)",
                targetFile.absolutePath,
            )
        }

        return gf
    }

    private fun renderPair(template: String, relativePath: String, model: Map<String, Any?>): GeneratedFile {
        val content = templateEngine.render(template, model, null)
        return GeneratedFile(path = relativePath, content = content)
    }

    private fun buildFreemarkerModel(c: BackendCodegenManifest, ddlSql: String? = null): Map<String, Any?> {
        val listCols = c.columns.filter { !it.isPk }
        val formCols = c.columns.filter { it.inBusinessForm }
        val tableCols =
            c.columns.sortedWith(
                compareByDescending<BackendCodegenManifestColumn> { it.isPk }
                    .thenBy { it.kotlinName },
            )
        val pascal = c.entityPascal
        val module = c.backendModuleName
        return mapOf(
            "entityPascal" to c.entityPascal,
            "entityCamel" to c.entityCamel,
            "restPath" to c.restPath,
            "pkField" to c.pkField,
            "pkTsType" to c.pkTsType,
            "moduleName" to module,
            "menuOrderNum" to topLevelSidebarOrderFor(module),
            "columns" to c.columns,
            "listColumns" to listCols,
            "tableColumns" to tableCols,
            "formColumns" to formCols,
            "requiredFormColumns" to formCols.filter { !it.nullable },
            "nameSearch" to c.columns.any { it.kotlinName == "name" && it.tsType == "string" },
            "hasImageField" to c.columns.any { it.formControl == FormControl.IMAGE_UPLOAD },
            "entityTitleZh" to moduleTitleZh(module, c.entityPascal),
            "pascal" to pascal,
            "ddlSql" to (ddlSql ?: ""),
        )
    }

    private fun moduleTitleZh(moduleSlug: String, entityPascal: String): String =
        when (moduleSlug.lowercase(Locale.US)) {
            "food" -> "食物"
            "cooking_steps" -> "烹饪步骤"
            else -> entityPascal
        }

    /**
     * [order_num] for root sidebar rows ([parent_id] IS NULL). Example seed uses ~1–99; snacks sit after Dashboard.
     */
    private fun topLevelSidebarOrderFor(moduleSlug: String): Int =
        when (moduleSlug.lowercase(Locale.US)) {
            "food" -> 4
            "cooking_steps" -> 5
            else -> 40 + kotlin.math.abs(moduleSlug.hashCode() % 39)
        }
}
