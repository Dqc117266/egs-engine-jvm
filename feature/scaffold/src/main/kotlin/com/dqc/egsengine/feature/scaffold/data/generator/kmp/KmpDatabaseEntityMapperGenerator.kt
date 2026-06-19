/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.DatabaseEntityDomainMapping
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Mode C: generates Entity to domain model mappers using name-based schema matching (`TopicEntity` to `TopicRespVO`).
 */
class KmpDatabaseEntityMapperGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(KmpDatabaseEntityMapperGenerator::class.java)

    fun generate(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        spec: SwaggerSpec,
        projectRoot: File?,
    ): List<GeneratedFile> {
        val rows = KmpDatabaseTemplateModels.buildRows(tables)
        val pairs =
            rows.mapNotNull { row ->
                val schema = DatabaseEntityDomainMapping.matchSchema(row.table, spec) ?: return@mapNotNull null
                DatabaseEntityDomainMapping
                    .buildMapperBlock(row.table, row.entityClassName, schema) { t ->
                        when (t) {
                            "BigDecimal" -> "Double"
                            "Instant" -> "Long"
                            else -> t
                        }
                    }?.let { row to it }
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

        val content =
            templateEngine.render(
                "kmp/database/EntityMapper.kt.ftl",
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
                "$moduleDir/src/commonMain/kotlin/$pkgPath/GeneratedEntityMappers.kt",
                content,
            ),
        )
    }

    /**
     * Maps Swagger domain model name (e.g. TopicRespVO) to table row, if a matching table exists.
     */
    fun buildDomainSchemaToRow(
        tables: List<TableSchema>,
        spec: SwaggerSpec,
    ): Map<String, KmpDatabaseTableRow> {
        val rows = KmpDatabaseTemplateModels.buildRows(tables)
        val map = mutableMapOf<String, KmpDatabaseTableRow>()
        for (row in rows) {
            val schema = DatabaseEntityDomainMapping.matchSchema(row.table, spec) ?: continue
            map[schema.name] = row
        }
        return map
    }

    companion object {
        fun pageListElementSchemaName(pageSchema: SwaggerSchema): String? {
            val listProp = pageSchema.properties.find { it.name == "list" } ?: return null
            val t = listProp.type
            return when (t) {
                is SwaggerType.ListType ->
                    when (val el = t.elementType) {
                        is SwaggerType.ModelRef -> el.name
                        else -> null
                    }
                else -> null
            }
        }
    }
}
