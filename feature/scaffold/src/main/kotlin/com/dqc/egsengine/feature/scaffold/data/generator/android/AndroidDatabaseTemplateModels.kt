/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.ColumnSchema
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema

/**
 * Table metadata for Android database FreeMarker templates (copied layout from KMP; independent package).
 */
data class AndroidDatabaseTableRow(
    val table: TableSchema,
    val entityClassName: String,
    val daoClassName: String,
    val daoPropertyName: String,
    val orderByColumnName: String,
    val pkColumnName: String,
    val pkPropertyName: String,
    val pkKotlinType: String,
    val prefixPascal: String,
    val entityColumns: List<Map<String, Any?>>,
)

object AndroidDatabaseTemplateModels {

    fun buildRows(tables: List<TableSchema>): List<AndroidDatabaseTableRow> =
        tables.map { buildRow(it) }

    fun buildRow(table: TableSchema): AndroidDatabaseTableRow {
        val base = SqlNaming.snakeToPascal(table.tableName)
        val entityClassName = "${base}Entity"
        val daoClassName = "${base}Dao"
        val daoPropertyName = daoClassName.replaceFirstChar { it.lowercase() }
        val sorted = sortColumns(table)
        val pkCol = sorted.firstOrNull { it.isPrimaryKey } ?: sorted.first()
        val pkPropertyName = SqlNaming.snakeToLowerCamel(pkCol.name)
        val entityColumns = sorted.map { col ->
            val autoGen = col.isAutoIncrement && col.kotlinType in setOf("Long", "Int")
            mapOf(
                "name" to col.name,
                "kotlinPropertyName" to SqlNaming.snakeToLowerCamel(col.name),
                "kotlinType" to col.kotlinType,
                "nullableMark" to if (col.nullable) "?" else "",
                "isPrimaryKey" to col.isPrimaryKey,
                "autoGenerate" to autoGen,
            )
        }
        return AndroidDatabaseTableRow(
            table = table,
            entityClassName = entityClassName,
            daoClassName = daoClassName,
            daoPropertyName = daoPropertyName,
            orderByColumnName = pkCol.name,
            pkColumnName = pkCol.name,
            pkPropertyName = pkPropertyName,
            pkKotlinType = pkCol.kotlinType,
            prefixPascal = SqlNaming.snakeToPascal(table.tableName),
            entityColumns = entityColumns,
        )
    }

    private fun sortColumns(table: TableSchema): List<ColumnSchema> {
        val pk = table.primaryKey
        return table.columns.sortedWith(
            compareBy<ColumnSchema> { col ->
                when {
                    pk != null && col.name == pk -> 0
                    col.isPrimaryKey -> 0
                    else -> 1
                }
            }.thenBy { it.name },
        )
    }
}
