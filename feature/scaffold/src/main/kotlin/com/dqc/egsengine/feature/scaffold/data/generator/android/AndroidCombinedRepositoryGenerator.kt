/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpRepositorySlices
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import java.io.File

/**
 * Combined `XRepository : XApiRepository, XDbRepository, ...` under `src/main/kotlin` (Android client).
 */
class AndroidCombinedRepositoryGenerator {

    fun generate(
        template: ModuleTemplate,
        subProjectRoot: File?,
        includeApi: Boolean,
        includeDb: Boolean,
        includePrefs: Boolean = false,
    ): GeneratedFile? {
        if (!includeApi && !includeDb && !includePrefs) return null
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
        val hasPrefs = includePrefs || (
            subProjectRoot != null && prefsRepositoryFile(subProjectRoot, template, pascal).exists()
            )
        if (!hasApi && !hasDb && !hasPrefs) return null

        val extends = buildList {
            if (hasApi) add(ctx.apiRepositoryName)
            if (hasDb) add(dbRepositoryName(pascal))
            if (hasPrefs) add(prefsRepositoryName(pascal))
        }
        val content = buildString {
            appendLine("package $domainPkg")
            appendLine()
            appendLine("internal interface ${ctx.combinedRepositoryName} : ${extends.joinToString(", ")}")
            appendLine()
        }
        return GeneratedFile(
            "$moduleDir/src/main/kotlin/$pkgPath/${ctx.combinedRepositoryName}.kt",
            content,
        )
    }

    private fun dbRepositoryName(pascal: String): String = "${pascal}DbRepository"

    private fun prefsRepositoryName(pascal: String): String = "${pascal}PrefsRepository"

    fun prefsRepositoryFile(root: File, template: ModuleTemplate, pascal: String): File {
        val pkgPath = template.packageName.replace('.', '/')
        return root.resolve(
            "feature/${template.name}/src/main/kotlin/$pkgPath/generate/domain/repository/${pascal}PrefsRepository.kt",
        )
    }

    private fun apiRepositoryFile(root: File, template: ModuleTemplate, pascal: String): File {
        val pkgPath = template.packageName.replace('.', '/')
        return root.resolve(
            "feature/${template.name}/src/main/kotlin/$pkgPath/generate/domain/repository/${pascal}ApiRepository.kt",
        )
    }

    private fun dbRepositoryFile(root: File, template: ModuleTemplate, pascal: String): File {
        val pkgPath = template.packageName.replace('.', '/')
        return root.resolve(
            "feature/${template.name}/src/main/kotlin/$pkgPath/generate/domain/repository/${pascal}DbRepository.kt",
        )
    }

    fun detectSlices(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
    ): KmpRepositorySlices {
        val pascal = SqlNaming.moduleNameToPascal(moduleName)
        val apiSwagger = androidSwaggerRepositoryFile(subProjectRoot, template, pascal).exists()
        val apiKmp = apiRepositoryFile(subProjectRoot, template, pascal).exists()
        val api = apiSwagger || apiKmp
        val db = dbRepositoryFile(subProjectRoot, template, pascal).exists()
        val prefs = prefsRepositoryFile(subProjectRoot, template, pascal).exists()
        return KmpRepositorySlices(hasApi = api, hasDb = db, hasPrefs = prefs)
    }

    private fun androidSwaggerRepositoryFile(root: File, template: ModuleTemplate, pascal: String): File {
        val pkgPath = template.packageName.replace('.', '/')
        return root.resolve(
            "feature/${template.name}/src/main/kotlin/$pkgPath/generate/domain/repository/${pascal}Repository.kt",
        )
    }
}
