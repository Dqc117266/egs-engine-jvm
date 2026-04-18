/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec

/**
 * Matches DDL tables to Swagger schemas and builds FreeMarker mapper blocks for
 * [KmpDatabaseEntityMapperGenerator] / [com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseEntityMapperGenerator].
 */
object DatabaseEntityDomainMapping {

    fun matchSchema(table: TableSchema, spec: SwaggerSpec): SwaggerSchema? {
        val base = SqlNaming.snakeToPascal(table.tableName)
        return spec.schemas.find { it.name == "${base}RespVO" }
            ?: spec.schemas.find {
                it.name.startsWith(base) && (it.name.contains("Resp") || it.name == base)
            }
    }

    fun buildMapperBlock(
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

    /**
     * Simple name of the Swagger VO when the table matches and at least one overlapping field exists; otherwise null (use Entity).
     */
    fun resolveDomainClassName(table: TableSchema, spec: SwaggerSpec?): String? {
        if (spec == null) return null
        val schema = matchSchema(table, spec) ?: return null
        val entityClassName = SqlNaming.snakeToPascal(table.tableName) + "Entity"
        val block = buildMapperBlock(table, entityClassName, schema) ?: return null
        return block["domainClassName"] as? String
    }
}
