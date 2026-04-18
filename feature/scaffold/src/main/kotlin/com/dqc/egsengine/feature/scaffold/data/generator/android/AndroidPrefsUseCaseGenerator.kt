/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpGeneratedDomainModuleIo
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.KmpPreferencesKotlinEmitter
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationMode
import com.dqc.egsengine.feature.scaffold.data.swagger.AndroidSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Emits Get/Set/Observe use cases for prefs repository methods (scalar or snapshot) under `src/main/kotlin`.
 */
class AndroidPrefsUseCaseGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(AndroidPrefsUseCaseGenerator::class.java)

    fun generate(
        template: ModuleTemplate,
        mode: PrefsGenerationMode,
        projectRoot: File?,
        subProjectRoot: File?,
    ): List<GeneratedFile> {
        val ctx = AndroidSwaggerGeneratorContext(template)
        val domainRepoImport = "${ctx.domainRepositoryPackage}.${ctx.repositoryName}"
        val useCasePkg = ctx.domainUseCasePackage
        val moduleDir = "feature/${template.name}"
        val pkgPath = useCasePkg.replace('.', '/')
        val useCaseNames = mutableListOf<String>()
        val files = mutableListOf<GeneratedFile>()

        fun add(
            useCaseName: String,
            invokeParams: String,
            returnType: String,
            repositoryCall: String,
            suspendInvoke: Boolean,
            needsFlowImport: Boolean,
            extraImports: Set<String> = emptySet(),
        ) {
            useCaseNames += useCaseName
            val content = templateEngine.render(
                "kmp/preferences/PrefsUseCase.kt.ftl",
                mapOf(
                    "useCasePackage" to useCasePkg,
                    "domainRepositoryImport" to domainRepoImport,
                    "combinedRepositoryName" to ctx.repositoryName,
                    "useCaseName" to useCaseName,
                    "invokeParams" to invokeParams,
                    "returnType" to returnType,
                    "repositoryCall" to repositoryCall,
                    "suspendInvoke" to suspendInvoke,
                    "needsFlowImport" to needsFlowImport,
                    "extraImports" to extraImports.sorted(),
                ),
                projectRoot,
            )
            files.add(GeneratedFile("$moduleDir/src/main/kotlin/$pkgPath/$useCaseName.kt", content))
        }

        when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val pascal = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                val kt = mode.field.kotlinType
                add(
                    "Get${pascal}UseCase",
                    "",
                    kt,
                    "get$pascal()",
                    suspendInvoke = true,
                    needsFlowImport = false,
                )
                add(
                    "Set${pascal}UseCase",
                    "value: $kt",
                    "Unit",
                    "set$pascal(value)",
                    suspendInvoke = true,
                    needsFlowImport = false,
                )
                add(
                    "Observe${pascal}UseCase",
                    "",
                    "Flow<$kt>",
                    "observe$pascal()",
                    suspendInvoke = false,
                    needsFlowImport = true,
                )
            }
            is PrefsGenerationMode.Snapshot -> {
                val snap = mode.snapshotClassName
                val modelPkg = "${template.packageName}.generate.data.datasource.preferences.model"
                val modelImport = "$modelPkg.$snap"
                add(
                    "Get${snap}UseCase",
                    "",
                    snap,
                    "get$snap()",
                    suspendInvoke = true,
                    needsFlowImport = false,
                    extraImports = setOf(modelImport),
                )
                add(
                    "Set${snap}UseCase",
                    "value: $snap",
                    "Unit",
                    "set$snap(value)",
                    suspendInvoke = true,
                    needsFlowImport = false,
                    extraImports = setOf(modelImport),
                )
                add(
                    "Observe${snap}UseCase",
                    "",
                    "Flow<$snap>",
                    "observe$snap()",
                    suspendInvoke = false,
                    needsFlowImport = true,
                    extraImports = setOf(modelImport),
                )
            }
        }

        val domainModulePath = "$moduleDir/src/main/kotlin/${ctx.generateDiPackage.replace('.', '/')}/GeneratedDomainModule.kt"
        val domainModule = resolveGeneratedDomainModuleContent(
            ctx = ctx,
            useCaseNames = useCaseNames,
            projectRoot = projectRoot,
            subProjectRoot = subProjectRoot,
            template = template,
        )
        files.add(GeneratedFile(domainModulePath, domainModule))

        logger.info("Generated {} Android prefs use case file(s) for module {}", files.size, template.name)
        return files
    }

    private fun resolveGeneratedDomainModuleContent(
        ctx: AndroidSwaggerGeneratorContext,
        useCaseNames: List<String>,
        projectRoot: File?,
        subProjectRoot: File?,
        template: ModuleTemplate,
    ): String {
        val pkgPath = template.packageName.replace('.', '/')
        val existing = subProjectRoot?.resolve(
            "feature/${template.name}/src/main/kotlin/$pkgPath/generate/di/GeneratedDomainModule.kt",
        )
        if (existing != null && existing.exists()) {
            var text = existing.readText()
            if (text.contains("egs-gen:swagger-usecases-begin")) {
                for (name in useCaseNames) {
                    val imp = "import ${ctx.domainUseCasePackage}.$name"
                    if (!text.contains(imp)) {
                        text = insertImportAfterPackage(text, imp)
                    }
                }
                return KmpGeneratedDomainModuleIo.replacePrefsUseCasesBlock(text, useCaseNames)
            }
        }
        return renderGeneratedDomainModule(ctx, useCaseNames, projectRoot)
    }

    private fun renderGeneratedDomainModule(
        ctx: AndroidSwaggerGeneratorContext,
        prefsUseCaseClassNames: List<String>,
        projectRoot: File?,
    ): String = templateEngine.render(
        "kmp/swagger/GeneratedDomainModule.kt.ftl",
        mapOf(
            "generateDiPackage" to ctx.generateDiPackage,
            "swaggerUseCases" to emptyList<Map<String, String>>(),
            "dbUseCases" to emptyList<Map<String, String>>(),
            "dbUseCaseImports" to emptyList<String>(),
            "prefsUseCases" to prefsUseCaseClassNames.map { mapOf("useCaseClass" to it) },
            "prefsUseCaseImports" to prefsUseCaseClassNames.map { "${ctx.domainUseCasePackage}.$it" },
        ),
        projectRoot,
    )

    private fun insertImportAfterPackage(text: String, importLine: String): String {
        val lines = text.lines().toMutableList()
        val pkgIdx = lines.indexOfFirst { it.startsWith("package ") }
        if (pkgIdx < 0) return "$importLine\n\n$text"
        var lastImport = pkgIdx
        for (j in pkgIdx + 1 until lines.size) {
            when {
                lines[j].startsWith("import ") -> lastImport = j
                lines[j].isBlank() -> continue
                else -> break
            }
        }
        lines.add(lastImport + 1, importLine)
        return lines.joinToString("\n").trimEnd() + "\n"
    }
}
