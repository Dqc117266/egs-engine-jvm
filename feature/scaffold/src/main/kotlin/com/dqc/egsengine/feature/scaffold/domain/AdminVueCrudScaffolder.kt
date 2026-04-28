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

/**
 * Emits Vue3 admin CRUD files from [BackendCodegenManifest] (written by backend `gen database`).
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
    )

    fun scaffoldFromCodegen(
        projectRoot: File,
        codegen: BackendCodegenManifest,
        dryRun: Boolean,
    ): Result {
        val adminCfg = workspaceConfigResolver.resolveAdmin(projectRoot)
        require(adminCfg.platform == Platform.VUE3) {
            "admin gen from-backend requires workspace project 'admin' with platform vue3; got ${adminCfg.platform}"
        }
        val adminRoot = projectRoot.resolve(adminCfg.path).normalize()
        val module = codegen.backendModuleName
        val model = buildFreemarkerModel(codegen)

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

        return Result(moduleName = module, files = files, dryRun = dryRun)
    }

    private fun renderPair(template: String, relativePath: String, model: Map<String, Any?>): GeneratedFile {
        val content = templateEngine.render(template, model, null)
        return GeneratedFile(path = relativePath, content = content)
    }

    private fun buildFreemarkerModel(c: BackendCodegenManifest): Map<String, Any?> {
        val listCols = c.columns.filter { !it.isPk }
        val formCols = c.columns.filter { it.inBusinessForm }
        val pascal = c.entityPascal
        return mapOf(
            "entityPascal" to c.entityPascal,
            "entityCamel" to c.entityCamel,
            "restPath" to c.restPath,
            "pkField" to c.pkField,
            "pkTsType" to c.pkTsType,
            "moduleName" to c.backendModuleName,
            "columns" to c.columns,
            "listColumns" to listCols,
            "formColumns" to formCols,
            "pascal" to pascal,
        )
    }
}
