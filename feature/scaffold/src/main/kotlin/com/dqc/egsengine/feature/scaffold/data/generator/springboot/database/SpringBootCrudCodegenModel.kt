/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot.database

import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.BackendCodegenManifest
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.BackendCodegenManifestColumn
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.FormControl
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.KotlinToTsTypeMapper
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
) {
    /** FreeMarker: `col.isPk` collides with JavaBean `isPk()`; use `col.pk` in `.ftl`. */
    val pk: Boolean get() = isPk
}

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

/** Shared column + trait computation for FreeMarker model and backend manifest. */
internal data class SharedCodegenContext(
    val traits: SchemaTraits,
    val codegenCols: List<CodegenColumn>,
    val entityPascal: String,
    val entityCamel: String,
    val restPath: String,
    val sharedRoot: String,
    val featureSegment: String,
    val featurePkg: String,
    val generatePkg: String,
    val pkCol: com.dqc.egsengine.feature.scaffold.data.ddl.model.ColumnSchema,
    val pkProp: String,
    val auditSnake: Set<String>,
    val entityBodyCols: List<CodegenColumn>,
    val finderNameCol: CodegenColumn?,
    val businessNonPk: List<CodegenColumn>,
    val cacheModelColumns: List<CodegenColumn>,
    val configHttpClientParamLine: String,
)

class SpringBootCodegenModelBuilder(
    private val traitInferrer: SchemaTraitInferrer,
) {
    private fun buildSharedContext(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions,
    ): SharedCodegenContext {
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
                nullable = col.nullable || (col.name.lowercase() in auditSnake),
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

        return SharedCodegenContext(
            traits = traits,
            codegenCols = codegenCols,
            entityPascal = entityPascal,
            entityCamel = entityCamel,
            restPath = restPath,
            sharedRoot = sharedRoot,
            featureSegment = featureSegment,
            featurePkg = featurePkg,
            generatePkg = generatePkg,
            pkCol = pkCol,
            pkProp = pkProp,
            auditSnake = auditSnake,
            entityBodyCols = entityBodyCols,
            finderNameCol = finderNameCol,
            businessNonPk = businessNonPk,
            cacheModelColumns = cacheModelColumns,
            configHttpClientParamLine = configHttpClientParamLine,
        )
    }

    fun buildBackendCodegenManifest(
        table: TableSchema,
        backendModuleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions,
    ): BackendCodegenManifest {
        val ctx = buildSharedContext(table, backendModuleName, config, options)
        val cols = ctx.codegenCols.map { c ->
            val tsType = KotlinToTsTypeMapper.toTsType(c.kotlinType)
            BackendCodegenManifestColumn(
                kotlinName = c.kotlinName,
                kotlinType = c.kotlinType,
                tsType = tsType,
                nullable = c.nullable,
                isPk = c.isPk,
                inBusinessForm = ctx.businessNonPk.any { it.kotlinName == c.kotlinName },
                formControl = FormControl.infer(c.kotlinName, c.kotlinType, tsType),
            )
        }
        return BackendCodegenManifest(
            entityPascal = ctx.entityPascal,
            entityCamel = ctx.entityCamel,
            restPath = ctx.restPath,
            tableSqlName = table.tableName,
            backendModuleName = backendModuleName,
            basePackage = config.basePackage,
            pkField = ctx.pkProp,
            pkTsType = KotlinToTsTypeMapper.toTsType(ctx.pkCol.kotlinType),
            columns = cols,
        )
    }

    fun build(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions,
    ): Map<String, Any?> {
        val ctx = buildSharedContext(table, moduleName, config, options)

        val importsDomain = linkedSetOf<String>()
        for (c in ctx.codegenCols) {
            if (!c.isPk && c.kotlinType == "Instant") importsDomain.add("java.time.Instant")
        }
        val kotlinImportsDomain = importsDomain.sorted()

        val notice =
            "${ "/" + "*" } WARNING: generated - do not edit. Regenerator overwrites this file. ${ "*" + "/" }"
        return mapOf(
            "warningGenerated" to notice,
            "moduleName" to moduleName,
            "entityPascal" to ctx.entityPascal,
            "entityCamel" to ctx.entityCamel,
            "tableSqlName" to table.tableName,
            "restPath" to ctx.restPath,
            "pkKotlinType" to ctx.pkCol.kotlinType,
            "pkKotlinName" to SqlNaming.snakeToLowerCamel(ctx.pkCol.name),
            "pkProp" to ctx.pkProp,
            "basePackage" to config.basePackage,
            "featurePackage" to ctx.featurePkg,
            "generatePackage" to ctx.generatePkg,
            "sharedRoot" to ctx.sharedRoot,
            "traits" to ctx.traits,
            "useJpaAuditingBase" to ctx.traits.useJpaAuditingBase,
            "hasStatus" to ctx.traits.hasStatusColumn,
            "featureSegment" to ctx.featureSegment,
            "columns" to ctx.codegenCols,
            "nonPkColumns" to ctx.codegenCols.filter { !it.isPk },
            "dtoNeedsInstantImport" to ctx.businessNonPk.any { it.kotlinType == "Instant" },
            "responseNeedsInstantImport" to ctx.codegenCols.any { !it.isPk && it.kotlinType == "Instant" },
            "entityBodyColumns" to ctx.entityBodyCols,
            "domainImports" to kotlinImportsDomain,
            "jpaFinderNameColumn" to ctx.finderNameCol,
            "options" to options,
            "cacheModelColumns" to ctx.cacheModelColumns,
            "hasNameStringColumn" to ctx.businessNonPk.any { it.kotlinName == "name" && it.kotlinType == "String" },
            "businessNonPkColumns" to ctx.businessNonPk,
            "featurePackagePath" to ctx.featurePkg.replace('.', '/'),
            "configHttpClientParamLine" to ctx.configHttpClientParamLine,
        )
    }
}
