/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.DatabaseEntityDomainMapping
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * One UseCase per DB CRUD method; depends on combined [KmpSwaggerGeneratorContext.combinedRepositoryName].
 */
class KmpDatabaseUseCaseGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(KmpDatabaseUseCaseGenerator::class.java)

    fun generate(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        projectRoot: File?,
        subProjectRoot: File? = null,
        spec: SwaggerSpec? = null,
    ): List<GeneratedFile> {
        require(tables.isNotEmpty()) { "tables required" }
        val ctx = KmpSwaggerGeneratorContext(template)
        val entityPackage = "${template.packageName}.generate.data.datasource.database.entity"
        val domainModelPackage = "${template.packageName}.generate.domain.model"
        val rows = KmpDatabaseTemplateModels.buildRows(tables)
        val moduleDir = "feature/${template.name}"
        val useCasePkg = ctx.domainUseCasePackage
        val domainRepoImport = "${ctx.domainRepositoryPackage}.${ctx.combinedRepositoryName}"
        val files = mutableListOf<GeneratedFile>()
        val useCaseNames = mutableListOf<String>()

        for (row in rows) {
            val p = row.prefixPascal
            val domainSimple = DatabaseEntityDomainMapping.resolveDomainClassName(row.table, spec)
            val e = domainSimple ?: row.entityClassName
            val rowTypeImport =
                if (domainSimple != null) "$domainModelPackage.$domainSimple" else "$entityPackage.${row.entityClassName}"
            val pk = row.pkPropertyName
            val pkt = row.pkKotlinType

            fun add(
                useCaseName: String,
                invokeParams: String,
                returnType: String,
                repositoryCall: String,
                extraImports: Set<String> = emptySet(),
            ) {
                useCaseNames += useCaseName
                val content = templateEngine.render(
                    "kmp/database/DbUseCase.kt.ftl",
                    mapOf(
                        "useCasePackage" to useCasePkg,
                        "domainRepositoryImport" to domainRepoImport,
                        "combinedRepositoryName" to ctx.combinedRepositoryName,
                        "useCaseName" to useCaseName,
                        "invokeParams" to invokeParams,
                        "returnType" to returnType,
                        "repositoryCall" to repositoryCall,
                        "extraImports" to extraImports.sorted(),
                    ),
                    projectRoot,
                )
                val pkgPath = useCasePkg.replace('.', '/')
                files.add(
                    GeneratedFile(
                        "$moduleDir/src/commonMain/kotlin/$pkgPath/$useCaseName.kt",
                        content,
                    ),
                )
            }

            add(
                "Get${p}AllUseCase",
                "",
                "List<$e>",
                "get${p}All()",
                setOf(rowTypeImport),
            )
            add(
                "Get${p}ByIdUseCase",
                "$pk: $pkt",
                "$e?",
                "get${p}ById($pk)",
                setOf(rowTypeImport),
            )
            add(
                "Count${p}UseCase",
                "",
                "Long",
                "count${p}()",
                emptySet(),
            )
            add(
                "Insert${p}UseCase",
                "entity: $e",
                "Unit",
                "insert${p}(entity)",
                setOf(rowTypeImport),
            )
            add(
                "InsertAll${p}UseCase",
                "entities: List<$e>",
                "Unit",
                "insertAll${p}(entities)",
                setOf(rowTypeImport),
            )
            add(
                "Update${p}UseCase",
                "entity: $e",
                "Unit",
                "update${p}(entity)",
                setOf(rowTypeImport),
            )
            add(
                "Delete${p}UseCase",
                "entity: $e",
                "Unit",
                "delete${p}(entity)",
                setOf(rowTypeImport),
            )
            add(
                "Delete${p}ByIdUseCase",
                "$pk: $pkt",
                "Unit",
                "delete${p}ById($pk)",
                emptySet(),
            )
            add(
                "DeleteAll${p}UseCase",
                "",
                "Unit",
                "deleteAll${p}()",
                emptySet(),
            )
        }

        val domainModulePath = "$moduleDir/src/commonMain/kotlin/${ctx.generateDiPackage.replace('.', '/')}/GeneratedDomainModule.kt"
        val domainModule = resolveGeneratedDomainModuleContent(
            ctx = ctx,
            useCaseNames = useCaseNames,
            projectRoot = projectRoot,
            subProjectRoot = subProjectRoot,
            template = template,
        )
        files.add(GeneratedFile(domainModulePath, domainModule))

        logger.info("Generated {} DB use case file(s) for module {}", files.size, template.name)
        return files
    }

    private fun resolveGeneratedDomainModuleContent(
        ctx: KmpSwaggerGeneratorContext,
        useCaseNames: List<String>,
        projectRoot: File?,
        subProjectRoot: File?,
        template: ModuleTemplate,
    ): String {
        val pkgPath = template.packageName.replace('.', '/')
        val existing = subProjectRoot?.resolve(
            "feature/${template.name}/src/commonMain/kotlin/$pkgPath/generate/di/GeneratedDomainModule.kt",
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
                return KmpGeneratedDomainModuleIo.replaceDbUseCasesBlock(text, useCaseNames)
            }
        }
        return renderGeneratedDomainModule(ctx, useCaseNames, projectRoot)
    }

    private fun renderGeneratedDomainModule(
        ctx: KmpSwaggerGeneratorContext,
        dbUseCaseClassNames: List<String>,
        projectRoot: File?,
    ): String = templateEngine.render(
        "kmp/swagger/GeneratedDomainModule.kt.ftl",
        mapOf(
            "generateDiPackage" to ctx.generateDiPackage,
            "swaggerUseCases" to emptyList<Map<String, String>>(),
            "dbUseCases" to dbUseCaseClassNames.map { mapOf("useCaseClass" to it) },
            "dbUseCaseImports" to dbUseCaseClassNames.map { "${ctx.domainUseCasePackage}.$it" },
            "prefsUseCases" to emptyList<Map<String, String>>(),
            "prefsUseCaseImports" to emptyList<String>(),
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
