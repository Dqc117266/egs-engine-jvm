/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import java.io.File

/**
 * Emits combined `TodoRepository : TodoApiRepository, TodoDbRepository` from existing sub-interface files.
 */
class KmpCombinedRepositoryGenerator {

    fun generate(
        template: ModuleTemplate,
        subProjectRoot: File?,
        includeApi: Boolean,
        includeDb: Boolean,
    ): GeneratedFile? {
        if (!includeApi && !includeDb) return null
        val ctx = KmpSwaggerGeneratorContext(template)
        val pascal = ctx.pascalModuleName
        val domainPkg = ctx.domainRepositoryPackage
        val moduleDir = "feature/${template.name}"
        val pkgPath = domainPkg.replace('.', '/')

        val hasApi = includeApi || (
            subProjectRoot != null && apiRepositoryFile(subProjectRoot, template, pascal).exists()
            )
        val hasDb = includeDb || (
            subProjectRoot != null && dbRepositoryFile(subProjectRoot, template, pascal).exists()
            )
        if (!hasApi && !hasDb) return null

        val extends = buildList {
            if (hasApi) add(ctx.apiRepositoryName)
            if (hasDb) add(dbRepositoryName(pascal))
        }
        val content = buildString {
            appendLine("package $domainPkg")
            appendLine()
            appendLine("internal interface ${ctx.combinedRepositoryName} : ${extends.joinToString(", ")}")
            appendLine()
        }
        return GeneratedFile(
            "$moduleDir/src/commonMain/kotlin/$pkgPath/${ctx.combinedRepositoryName}.kt",
            content,
        )
    }

    private fun dbRepositoryName(pascal: String): String = "${pascal}DbRepository"

    private fun apiRepositoryFile(root: File, template: ModuleTemplate, pascal: String): File {
        val pkgPath = template.packageName.replace('.', '/')
        return root.resolve(
            "feature/${template.name}/src/commonMain/kotlin/$pkgPath/generate/domain/repository/${pascal}ApiRepository.kt",
        )
    }

    private fun dbRepositoryFile(root: File, template: ModuleTemplate, pascal: String): File {
        val pkgPath = template.packageName.replace('.', '/')
        return root.resolve(
            "feature/${template.name}/src/commonMain/kotlin/$pkgPath/generate/domain/repository/${pascal}DbRepository.kt",
        )
    }

    /**
     * Resolves whether API / DB slices exist on disk (for orchestration).
     */
    fun detectSlices(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
    ): Pair<Boolean, Boolean> {
        val pascal = SqlNaming.moduleNameToPascal(moduleName)
        val api = apiRepositoryFile(subProjectRoot, template, pascal).exists()
        val db = dbRepositoryFile(subProjectRoot, template, pascal).exists()
        return api to db
    }
}
