/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot.database

import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema

data class CodegenColumn(
    val sqlName: String,
    val kotlinName: String,
    val kotlinType: String,
    val nullable: Boolean,
    val maxLength: Int?,
    val isPk: Boolean,
    /** Suffix for property default, e.g. ` = 0L`, ` = null`, or empty. */
    val defaultSuffix: String,
)

object SpringBootCodegenTableValidator {
    fun validateTableForV1(table: TableSchema) {
        val pk = table.primaryKey ?: error("Table '${table.tableName}' must declare a PRIMARY KEY")
        val pkCol = table.columns.firstOrNull { it.name.equals(pk, ignoreCase = true) }
            ?: error("Primary key column '$pk' missing in column list for '${table.tableName}'")
        require(pkCol.kotlinType == "Long") {
            "Spring Boot DB codegen v1 supports Long primary keys only; got ${pkCol.kotlinType} for $pk"
        }
    }
}

class SpringBootCodegenModelBuilder(
    private val traitInferrer: SchemaTraitInferrer,
) {
    fun build(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions,
    ): Map<String, Any?> {
        SpringBootCodegenTableValidator.validateTableForV1(table)
        val traits = traitInferrer.infer(table, options)
        val entityPascal = SqlNaming.tableToEntityPascal(table.tableName)
        val entityCamel = entityPascal.replaceFirstChar { it.lowercase() }
        val restPath = SqlNaming.tableToRestPath(table.tableName)
        val sharedRoot = "${config.basePackage}.shared"
        val featureSegment = moduleName.replace("-", "").replace(" ", "")
        val featurePkg = "${config.basePackage}.feature.$featureSegment"
        val generatePkg = "$featurePkg.generate"
        val pkCol = table.columns.first { it.name.equals(table.primaryKey, ignoreCase = true) }
        val pkProp = SqlNaming.snakeToLowerCamel(pkCol.name)

        val auditSnake = setOf("created_at", "updated_at")
        val codegenCols = table.columns.map { col ->
            val isPk = col.isPrimaryKey || col.name.equals(table.primaryKey, ignoreCase = true)
            val kn = SqlNaming.snakeToLowerCamel(col.name)
            val defaultSuffix =
                when {
                    isPk && col.kotlinType == "Long" -> " = 0L"
                    isPk -> " = 0"
                    col.name.lowercase() in auditSnake -> " = null"
                    kn == "status" && col.kotlinType == "Int" -> " = 1"
                    col.nullable -> " = null"
                    else -> ""
                }
            CodegenColumn(
                sqlName = col.name,
                kotlinName = kn,
                kotlinType = col.kotlinType,
                nullable = col.nullable,
                maxLength = col.length,
                isPk = isPk,
                defaultSuffix = defaultSuffix,
            )
        }

        val entityBodyCols = codegenCols.filter {
            !it.isPk && !(traits.useJpaAuditingBase && it.sqlName.lowercase() in auditSnake)
        }

        val finderNameCol = entityBodyCols.firstOrNull {
            it.kotlinName == "name" && it.kotlinType == "String"
        }

        val importsDomain = linkedSetOf<String>()
        for (c in codegenCols) {
            if (!c.isPk && c.kotlinType == "Instant") importsDomain.add("java.time.Instant")
        }
        val kotlinImportsDomain = importsDomain.sorted()

        val businessNonPk = codegenCols.filter {
            !it.isPk && it.sqlName.lowercase() !in auditSnake
        }

        val pkCodegen = codegenCols.first { it.isPk }
        val cacheModelColumns = listOf(pkCodegen) + businessNonPk

        val configHttpClientParamLine = buildString {
            append("        @Value(\"")
            append('\\')
            append('$')
            append("{egs.feature.")
            append(moduleName)
            append(".http.base-url:}\") baseUrl: String,")
        }

        val notice = "${ "/" + "*" } WARNING: generated - do not edit. Regenerator overwrites this file. ${ "*" + "/" }"
        return mapOf(
            "warningGenerated" to notice,
            "moduleName" to moduleName,
            "entityPascal" to entityPascal,
            "entityCamel" to entityCamel,
            "tableSqlName" to table.tableName,
            "restPath" to restPath,
            "pkKotlinType" to pkCol.kotlinType,
            "pkKotlinName" to SqlNaming.snakeToLowerCamel(pkCol.name),
            "pkProp" to pkProp,
            "basePackage" to config.basePackage,
            "featurePackage" to featurePkg,
            "generatePackage" to generatePkg,
            "sharedRoot" to sharedRoot,
            "traits" to traits,
            "useJpaAuditingBase" to traits.useJpaAuditingBase,
            "hasStatus" to traits.hasStatusColumn,
            "featureSegment" to featureSegment,
            "columns" to codegenCols,
            "nonPkColumns" to codegenCols.filter { !it.isPk },
            "dtoNeedsInstantImport" to businessNonPk.any { it.kotlinType == "Instant" },
            "responseNeedsInstantImport" to codegenCols.any { !it.isPk && it.kotlinType == "Instant" },
            "entityBodyColumns" to entityBodyCols,
            "domainImports" to kotlinImportsDomain,
            "jpaFinderNameColumn" to finderNameCol,
            "options" to options,
            "cacheModelColumns" to cacheModelColumns,
            "hasNameStringColumn" to businessNonPk.any { it.kotlinName == "name" && it.kotlinType == "String" },
            "businessNonPkColumns" to businessNonPk,
            "featurePackagePath" to featurePkg.replace('.', '/'),
            "configHttpClientParamLine" to configHttpClientParamLine,
        )
    }
}
