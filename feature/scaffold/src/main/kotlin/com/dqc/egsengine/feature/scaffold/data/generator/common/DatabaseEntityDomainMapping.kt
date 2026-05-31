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
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType
import com.dqc.egsengine.feature.scaffold.data.swagger.PrimitiveKind

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

    /**
     * Build mapper block with type-aware conversion.
     * [entityTypeMapper] converts DDL kotlinType to the target platform Entity type
     * (e.g. KMP maps Instant→Long, BigDecimal→Double; Spring keeps as-is).
     */
    fun buildMapperBlock(
        table: TableSchema,
        entityClassName: String,
        schema: SwaggerSchema,
        entityTypeMapper: (String) -> String = { it },
    ): Map<String, Any?>? {
        val domainClassName = schema.name
        val columnByKotlinProp = table.columns.associateBy { SqlNaming.snakeToLowerCamel(it.name) }

        val toDomainLines = mutableListOf<String>()
        val toEntityLines = mutableListOf<String>()

        for (prop in schema.properties) {
            val col = columnByKotlinProp[prop.name] ?: continue
            val entityType = entityTypeMapper(col.kotlinType)
            val domainType = swaggerTypeToKotlin(prop.type, prop.required)
            val lhs = prop.name

            if (entityType == domainType) {
                // Types match: simple assignment
                toDomainLines.add("    $lhs = $lhs")
                toEntityLines.add("    $lhs = $lhs")
            } else {
                // Try to generate conversion expressions
                val toDomainExpr = convertToDomain(lhs, entityType, domainType)
                val toEntityExpr = convertToEntity(lhs, domainType, entityType)
                if (toDomainExpr != null && toEntityExpr != null) {
                    toDomainLines.add("    $lhs = $toDomainExpr")
                    toEntityLines.add("    $lhs = $toEntityExpr")
                }
                // If conversion not possible, skip the field
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

    private fun swaggerTypeToKotlin(type: SwaggerType, required: Boolean): String {
        val base = when (type) {
            is SwaggerType.Primitive -> when (type.kind) {
                PrimitiveKind.STRING -> "String"
                PrimitiveKind.INT -> "Int"
                PrimitiveKind.LONG -> "Long"
                PrimitiveKind.DOUBLE -> "Double"
                PrimitiveKind.BOOLEAN -> "Boolean"
            }
            else -> "String"
        }
        return if (required) base else "$base?"
    }

    /** Convert an Entity field value to Domain model type. */
    private fun convertToDomain(expr: String, fromType: String, toType: String): String? {
        return when {
            fromType == toType -> expr
            fromType == "Long" && toType == "String?" -> "\"\"\$expr\"\""
            fromType == "Long" && toType == "String" -> "\"\"\$expr\"\""
            fromType == "Double" && toType == "String?" -> "\"\"\$expr\"\""
            fromType == "Int" && toType == "Long" -> "\$expr.toLong()"
            fromType == "Int" && toType == "Long?" -> "\$expr.toLong()"
            fromType == "Long" && toType == "Int" -> "\$expr.toInt()"
            fromType == "Double" && toType == "Int" -> "\$expr.toInt()"
            fromType == "Int" && toType == "Double" -> "\$expr.toDouble()"
            fromType == "Long" && toType == "Double" -> "\$expr.toDouble()"
            fromType == "Double" && toType == "Long" -> "\$expr.toLong()"
            else -> null
        }
    }

    /** Convert a Domain model field value to Entity type. */
    private fun convertToEntity(expr: String, fromType: String, toType: String): String? {
        return when {
            fromType == toType -> expr
            fromType == "String?" && toType == "Long" -> "\$expr?.toLongOrNull() ?: 0L"
            fromType == "String" && toType == "Long" -> "\$expr.toLongOrNull() ?: 0L"
            fromType == "String?" && toType == "Double" -> "\$expr?.toDoubleOrNull() ?: 0.0"
            fromType == "String" && toType == "Double" -> "\$expr.toDoubleOrNull() ?: 0.0"
            fromType == "Long" && toType == "Int" -> "\$expr.toInt()"
            fromType == "Long?" && toType == "Int" -> "(\$expr ?: 0L).toInt()"
            fromType == "Double" && toType == "Int" -> "\$expr.toInt()"
            fromType == "Int" && toType == "Long" -> "\$expr.toLong()"
            fromType == "Int" && toType == "Double" -> "\$expr.toDouble()"
            fromType == "Long" && toType == "Double" -> "\$expr.toDouble()"
            fromType == "Double" && toType == "Long" -> "\$expr.toLong()"
            else -> null
        }
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
