/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Entity ? domain mappers for Android (`main`), same matching rules as KMP.
 */
class AndroidDatabaseEntityMapperGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(AndroidDatabaseEntityMapperGenerator::class.java)

    fun generate(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        spec: SwaggerSpec,
        projectRoot: File?,
    ): List<GeneratedFile> {
        val rows = AndroidDatabaseTemplateModels.buildRows(tables)
        val pairs = rows.mapNotNull { row -> matchSchema(row.table, spec)?.let { row to it } }
            .mapNotNull { (row, schema) ->
                buildMapperBlock(row.table, row.entityClassName, schema)?.let { row to it }
            }
        if (pairs.isEmpty()) {
            logger.warn("No Swagger schemas matched DDL tables with overlapping fields; skipping entity mappers.")
            return emptyList()
        }

        val pkg = template.packageName
        val mapperPackage = "$pkg.generate.data.datasource.database.mapper"
        val entityPackage = "$pkg.generate.data.datasource.database.entity"
        val domainPackage = "$pkg.generate.domain.model"
        val moduleDir = "feature/${template.name}"

        val blocks = pairs.map { (_, block) -> block }

        val entityImports = pairs.map { "$entityPackage.${it.first.entityClassName}" }.distinct().sorted()
        val domainImports = pairs.map { "$domainPackage.${it.second["domainClassName"] as String}" }.distinct().sorted()

        val content = templateEngine.render(
            "android/database/EntityMapper.kt.ftl",
            mapOf(
                "mapperPackageName" to mapperPackage,
                "entityImports" to entityImports,
                "domainImports" to domainImports,
                "mapperBlocks" to blocks,
            ),
            projectRoot,
        )

        val pkgPath = mapperPackage.replace('.', '/')
        return listOf(
            GeneratedFile(
                "$moduleDir/src/main/kotlin/$pkgPath/GeneratedEntityMappers.kt",
                content,
            ),
        )
    }

    private fun matchSchema(table: TableSchema, spec: SwaggerSpec): SwaggerSchema? {
        val base = SqlNaming.snakeToPascal(table.tableName)
        return spec.schemas.find { it.name == "${base}RespVO" }
            ?: spec.schemas.find {
                it.name.startsWith(base) && (it.name.contains("Resp") || it.name == base)
            }
    }

    private fun buildMapperBlock(
        table: TableSchema,
        entityClassName: String,
        schema: SwaggerSchema,
    ): Map<String, Any?>? {
        val domainClassName = schema.name
        val columnByKotlinProp = table.columns.associateBy { SqlNaming.snakeToLowerCamel(it.name) }

        val toDomainLines = mutableListOf<String>()
        val toEntityLines = mutableListOf<String>()

        for (prop in schema.properties) {
            val col = columnByKotlinProp[prop.name]
            if (col != null) {
                val lhs = prop.name
                toDomainLines.add("    $lhs = $lhs")
                toEntityLines.add("    $lhs = $lhs")
            }
        }
        if (toDomainLines.isEmpty()) return null

        val toDomainBody = toDomainLines.joinToString(",\n")
        val toEntityBody = toEntityLines.joinToString(",\n")

        return mapOf(
            "entityClassName" to entityClassName,
            "domainClassName" to domainClassName,
            "toDomainBody" to toDomainBody,
            "toEntityBody" to toEntityBody,
        )
    }

    companion object {
        fun pageListElementSchemaName(pageSchema: SwaggerSchema): String? {
            val listProp = pageSchema.properties.find { it.name == "list" } ?: return null
            val t = listProp.type
            return when (t) {
                is SwaggerType.ListType -> when (val el = t.elementType) {
                    is SwaggerType.ModelRef -> el.name
                    else -> null
                }
                else -> null
            }
        }
    }
}
