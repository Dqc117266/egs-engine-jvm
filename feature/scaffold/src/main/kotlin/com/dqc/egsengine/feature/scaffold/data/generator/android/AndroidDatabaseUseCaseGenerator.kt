/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

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
 * DB CRUD use cases under Android `main`; repository type defaults to combined [XRepository], or [XDbRepository] when API sync exists.
 */
class AndroidDatabaseUseCaseGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(AndroidDatabaseUseCaseGenerator::class.java)

    fun generate(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        projectRoot: File?,
        subProjectRoot: File? = null,
        useCaseRepositorySimpleName: String? = null,
        spec: SwaggerSpec? = null,
    ): List<GeneratedFile> {
        require(tables.isNotEmpty()) { "tables required" }
        val pascal = SqlNaming.moduleNameToPascal(template.name)
        val root = "${template.packageName}.generate"
        val domainUseCasePackage = "$root.domain.usecase"
        val domainRepositoryPackage = "$root.domain.repository"
        val generateDiPackage = "$root.di"
        val combinedRepositoryName = "${pascal}Repository"
        val repositoryTypeName = useCaseRepositorySimpleName ?: combinedRepositoryName
        val domainRepoImport = "$domainRepositoryPackage.$repositoryTypeName"

        val entityPackage = "${template.packageName}.generate.data.datasource.database.entity"
        val domainModelPackage = "${template.packageName}.generate.domain.model"
        val rows = AndroidDatabaseTemplateModels.buildRows(tables)
        val moduleDir = "feature/${template.name}"
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
                    "android/database/DbUseCase.kt.ftl",
                    mapOf(
                        "useCasePackage" to domainUseCasePackage,
                        "domainRepositoryImport" to domainRepoImport,
                        "combinedRepositoryName" to repositoryTypeName,
                        "useCaseName" to useCaseName,
                        "invokeParams" to invokeParams,
                        "returnType" to returnType,
                        "repositoryCall" to repositoryCall,
                        "extraImports" to extraImports.sorted(),
                    ),
                    projectRoot,
                )
                val pkgPath = domainUseCasePackage.replace('.', '/')
                files.add(
                    GeneratedFile(
                        "$moduleDir/src/main/kotlin/$pkgPath/$useCaseName.kt",
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

        val domainModulePath = "$moduleDir/src/main/kotlin/${generateDiPackage.replace('.', '/')}/GeneratedDomainModule.kt"
        val domainModule = resolveGeneratedDomainModuleContent(
            domainUseCasePackage = domainUseCasePackage,
            generateDiPackage = generateDiPackage,
            useCaseNames = useCaseNames,
            projectRoot = projectRoot,
            subProjectRoot = subProjectRoot,
            template = template,
        )
        files.add(GeneratedFile(domainModulePath, domainModule))

        logger.info("Generated {} Android DB use case file(s) for module {}", files.size, template.name)
        return files
    }

    private fun resolveGeneratedDomainModuleContent(
        domainUseCasePackage: String,
        generateDiPackage: String,
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
                    val imp = "import $domainUseCasePackage.$name"
                    if (!text.contains(imp)) {
                        text = insertImportAfterPackage(text, imp)
                    }
                }
                return AndroidGeneratedDomainModuleIo.replaceDbUseCasesBlock(text, useCaseNames)
            }
        }
        return renderGeneratedDomainModule(generateDiPackage, domainUseCasePackage, useCaseNames, projectRoot)
    }

    private fun renderGeneratedDomainModule(
        generateDiPackage: String,
        domainUseCasePackage: String,
        dbUseCaseClassNames: List<String>,
        projectRoot: File?,
    ): String = templateEngine.render(
        "kmp/swagger/GeneratedDomainModule.kt.ftl",
        mapOf(
            "generateDiPackage" to generateDiPackage,
            "swaggerUseCases" to emptyList<Map<String, String>>(),
            "dbUseCases" to dbUseCaseClassNames.map { mapOf("useCaseClass" to it) },
            "dbUseCaseImports" to dbUseCaseClassNames.map { "$domainUseCasePackage.$it" },
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
