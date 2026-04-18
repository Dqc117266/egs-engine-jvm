/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.DatabaseEntityDomainMapping
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
        val pairs = rows.mapNotNull { row ->
            val schema = DatabaseEntityDomainMapping.matchSchema(row.table, spec) ?: return@mapNotNull null
            DatabaseEntityDomainMapping.buildMapperBlock(row.table, row.entityClassName, schema)?.let { row to it }
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
