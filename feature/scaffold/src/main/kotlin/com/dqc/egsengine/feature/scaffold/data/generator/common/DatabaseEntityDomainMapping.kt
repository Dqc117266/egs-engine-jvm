/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.PrimitiveKind
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSchema
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType

/**
 * Matches DDL tables to Swagger schemas and builds FreeMarker mapper blocks for
 * [KmpDatabaseEntityMapperGenerator] / [com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseEntityMapperGenerator].
 */
object DatabaseEntityDomainMapping {
    fun matchSchema(
        table: TableSchema,
        spec: SwaggerSpec,
    ): SwaggerSchema? {
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
            val rawEntityType = entityTypeMapper(col.kotlinType)
            val entityNullable = col.nullable
            val domainType = swaggerTypeToKotlin(prop.type, prop.required)
            val domainNullable = domainType.endsWith("?")
            val lhs = prop.name

            // Strip nullable for base type comparison
            val entityBase = rawEntityType
            val domainBase = domainType.trimEnd('?')
            val nullableMatch = entityNullable == domainNullable

            if (entityBase == domainBase) {
                // Same base type - always assignable (nullable mismatch is safe in Kotlin)
                toDomainLines.add("    $lhs = $lhs")
                toEntityLines.add("    $lhs = $lhs")
            } else {
                // Try to generate conversion expressions
                val toDomainExpr = convertToDomain(lhs, rawEntityType, domainType)
                val toEntityExpr = convertToEntity(lhs, domainType, rawEntityType)
                if (toDomainExpr != null && toEntityExpr != null) {
                    toDomainLines.add("    $lhs = $toDomainExpr")
                    toEntityLines.add("    $lhs = $toEntityExpr")
                }
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

    private fun swaggerTypeToKotlin(
        type: SwaggerType,
        required: Boolean,
    ): String {
        val base =
            when (type) {
                is SwaggerType.Primitive ->
                    when (type.kind) {
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
    private fun convertToDomain(
        expr: String,
        fromType: String,
        toType: String,
    ): String? {
        return buildString {
            when {
                fromType == toType -> append(expr)
                fromType == "Long" && (toType == "String?" || toType == "String") -> {
                    append(expr)
                    append(".toString()")
                }
                fromType == "Double" && (toType == "String?" || toType == "String") -> {
                    append(expr)
                    append(".toString()")
                }
                fromType == "Int" && (toType == "Long" || toType == "Long?") -> {
                    append(expr)
                    append(".toLong()")
                }
                fromType == "Long" && toType == "Int" -> {
                    append(expr)
                    append(".toInt()")
                }
                fromType == "Double" && toType == "Int" -> {
                    append(expr)
                    append(".toInt()")
                }
                fromType == "Int" && toType == "Double" -> {
                    append(expr)
                    append(".toDouble()")
                }
                fromType == "Long" && toType == "Double" -> {
                    append(expr)
                    append(".toDouble()")
                }
                fromType == "Double" && toType == "Long" -> {
                    append(expr)
                    append(".toLong()")
                }
                else -> return null
            }
        }.toString()
    }

    /** Convert a Domain model field value to Entity type. */
    private fun convertToEntity(
        expr: String,
        fromType: String,
        toType: String,
    ): String? {
        return buildString {
            when {
                fromType == toType -> append(expr)
                fromType == "String?" && toType == "Long" -> {
                    append(expr)
                    append("?.toLongOrNull() ?: 0L")
                }
                fromType == "String" && toType == "Long" -> {
                    append(expr)
                    append(".toLongOrNull() ?: 0L")
                }
                fromType == "String?" && toType == "Double" -> {
                    append(expr)
                    append("?.toDoubleOrNull() ?: 0.0")
                }
                fromType == "String" && toType == "Double" -> {
                    append(expr)
                    append(".toDoubleOrNull() ?: 0.0")
                }
                fromType == "Long" && toType == "Int" -> {
                    append(expr)
                    append(".toInt()")
                }
                fromType == "Long?" && toType == "Int" -> {
                    append("(")
                    append(expr)
                    append(" ?: 0L).toInt()")
                }
                fromType == "Double" && toType == "Int" -> {
                    append(expr)
                    append(".toInt()")
                }
                fromType == "Int" && toType == "Long" -> {
                    append(expr)
                    append(".toLong()")
                }
                fromType == "Int" && toType == "Double" -> {
                    append(expr)
                    append(".toDouble()")
                }
                fromType == "Long" && toType == "Double" -> {
                    append(expr)
                    append(".toDouble()")
                }
                fromType == "Double" && toType == "Long" -> {
                    append(expr)
                    append(".toLong()")
                }
                else -> return null
            }
        }.toString()
    }

    /**
     * Simple name of the Swagger VO when the table matches and at least one overlapping field exists; otherwise null (use Entity).
     */
    fun resolveDomainClassName(
        table: TableSchema,
        spec: SwaggerSpec?,
    ): String? {
        if (spec == null) return null
        val schema = matchSchema(table, spec) ?: return null
        val entityClassName = SqlNaming.snakeToPascal(table.tableName) + "Entity"
        val block = buildMapperBlock(table, entityClassName, schema) ?: return null
        return block["domainClassName"] as? String
    }
}
