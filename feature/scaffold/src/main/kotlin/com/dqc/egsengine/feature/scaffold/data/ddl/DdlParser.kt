/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.ddl

import com.dqc.egsengine.feature.scaffold.data.ddl.model.ColumnSchema
import com.dqc.egsengine.feature.scaffold.data.ddl.model.IndexSchema
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import org.slf4j.LoggerFactory

/**
 * Parses SQL `CREATE TABLE` statements into [TableSchema] models.
 * Supports MySQL-style DDL (comments stripped); multiple tables per file.
 */
class DdlParser {

    private val logger = LoggerFactory.getLogger(DdlParser::class.java)

    fun parse(sql: String): List<TableSchema> {
        val stripped = stripSqlComments(sql)
        val blocks = extractCreateTableBlocks(stripped)
        val tables = blocks.map { (tableName, body) -> parseTableBody(tableName, body) }
        logger.info("DdlParser parsed {} table(s)", tables.size)
        return tables
    }

    fun parseFile(file: java.io.File): List<TableSchema> {
        require(file.exists()) { "DDL file not found: ${file.absolutePath}" }
        return parse(file.readText())
    }

    private fun stripSqlComments(sql: String): String {
        var s = sql
        // block comments /* */
        s = Regex("""/\*[\s\S]*?\*/""").replace(s, " ")
        // line comments --
        s = s.lines().joinToString("\n") { line ->
            val idx = line.indexOf("--")
            if (idx >= 0) line.substring(0, idx) else line
        }
        return s
    }

    private fun extractCreateTableBlocks(sql: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var i = 0
        val upper = sql.length
        while (i < upper) {
            val match = Regex("CREATE\\s+TABLE\\s+", RegexOption.IGNORE_CASE).find(sql, i) ?: break
            var j = match.range.last + 1
            j = skipWs(sql, j)
            val (tableName, afterName) = readIdentifier(sql, j)
            if (tableName.isEmpty()) {
                i = match.range.first + 1
                continue
            }
            j = skipWs(sql, afterName)
            if (j >= sql.length || sql[j] != '(') {
                i = afterName
                continue
            }
            j++
            val bodyStart = j
            var depth = 1
            while (j < sql.length && depth > 0) {
                when (sql[j]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                j++
            }
            if (depth != 0) {
                logger.warn("Unbalanced parentheses in CREATE TABLE {}", tableName)
                break
            }
            val body = sql.substring(bodyStart, j - 1)
            result.add(tableName to body)
            i = j
        }
        return result
    }

    private fun skipWs(s: String, start: Int): Int {
        var j = start
        while (j < s.length && s[j].isWhitespace()) j++
        return j
    }

    private fun readIdentifier(s: String, start: Int): Pair<String, Int> {
        var j = skipWs(s, start)
        if (j >= s.length) return "" to j
        return when (s[j]) {
            '`' -> {
                val end = s.indexOf('`', j + 1)
                if (end < 0) return "" to j
                s.substring(j + 1, end) to (end + 1)
            }
            '"' -> {
                val end = s.indexOf('"', j + 1)
                if (end < 0) return "" to j
                s.substring(j + 1, end) to (end + 1)
            }
            else -> {
                val from = j
                while (j < s.length && (s[j].isLetterOrDigit() || s[j] == '_')) j++
                s.substring(from, j) to j
            }
        }
    }

    private fun parseTableBody(tableName: String, body: String): TableSchema {
        val lines = splitTableBodyLines(body)
        val pkColumns = mutableSetOf<String>()
        val indexList = mutableListOf<IndexSchema>()
        val columnLines = mutableListOf<String>()

        for (raw in lines) {
            val line = raw.trim().trimEnd(',', ';').trim()
            if (line.isEmpty()) continue
            val upper = line.uppercase()
            when {
                upper.startsWith("PRIMARY KEY") -> {
                    val cols = extractParenColumnList(line)
                    pkColumns.addAll(cols)
                }
                upper.startsWith("UNIQUE KEY") || upper.startsWith("KEY ") || upper.startsWith("INDEX ") -> {
                    // optional: parse indexes ¡ª skip for Room v1
                }
                upper.startsWith("CONSTRAINT") || upper.startsWith("FOREIGN KEY") -> continue
                else -> columnLines.add(line)
            }
        }

        val columns = columnLines.mapNotNull { parseColumnLine(it, pkColumns) }
        val primaryKey = pkColumns.singleOrNull()
            ?: columns.firstOrNull { it.isPrimaryKey }?.name

        return TableSchema(
            tableName = tableName,
            columns = columns,
            primaryKey = primaryKey,
            indexes = indexList,
            comment = null,
        )
    }

    private fun extractParenColumnList(line: String): List<String> {
        val open = line.indexOf('(')
        val close = line.lastIndexOf(')')
        if (open < 0 || close <= open) return emptyList()
        return line.substring(open + 1, close)
            .split(',')
            .map { it.trim().trim('`', '"') }
            .filter { it.isNotEmpty() }
    }

    private fun splitTableBodyLines(body: String): List<String> {
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var depth = 0
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when (c) {
                '(' -> {
                    depth++
                    sb.append(c)
                }
                ')' -> {
                    depth--
                    sb.append(c)
                }
                ',' -> {
                    if (depth == 0) {
                        parts.add(sb.toString())
                        sb.clear()
                    } else {
                        sb.append(c)
                    }
                }
                else -> sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) parts.add(sb.toString())
        return parts
    }

    private fun parseColumnLine(line: String, tablePk: Set<String>): ColumnSchema? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val tokens = tokenizeColumnDef(trimmed)
        if (tokens.isEmpty()) return null
        val colName = tokens[0].trim('`', '"')
        if (tokens.size < 2) return null
        val sqlTypeToken = tokens[1]
        val upperRest = tokens.drop(2).joinToString(" ").uppercase()

        val isPkInLine = upperRest.contains("PRIMARY KEY")
        val autoInc = upperRest.contains("AUTO_INCREMENT") || upperRest.contains("AUTOINCREMENT")
        val notNull = upperRest.contains("NOT NULL") || isPkInLine || autoInc
        val isPk = colName in tablePk || isPkInLine
        val nullable = if (isPk) false else !notNull

        val defaultVal = extractDefault(trimmed)

        val (kotlinType, length) = mapSqlTypeToKotlin(sqlTypeToken)

        return ColumnSchema(
            name = colName,
            sqlType = sqlTypeToken,
            kotlinType = kotlinType,
            nullable = nullable,
            isPrimaryKey = isPk,
            isAutoIncrement = autoInc,
            defaultValue = defaultVal,
            comment = null,
            length = length,
        )
    }

    private fun tokenizeColumnDef(line: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < line.length) {
            i = skipWs(line, i)
            if (i >= line.length) break
            when (line[i]) {
                '`' -> {
                    val end = line.indexOf('`', i + 1)
                    if (end < 0) break
                    out.add(line.substring(i + 1, end))
                    i = end + 1
                }
                '"' -> {
                    val end = line.indexOf('"', i + 1)
                    if (end < 0) break
                    out.add(line.substring(i + 1, end))
                    i = end + 1
                }
                else -> {
                    val start = i
                    while (i < line.length && !line[i].isWhitespace() && line[i] != ',') i++
                    out.add(line.substring(start, i))
                }
            }
        }
        return out
    }

    private fun extractDefault(line: String): String? {
        val upper = line.uppercase()
        val idx = upper.indexOf("DEFAULT")
        if (idx < 0) return null
        var rest = line.substring(idx + "DEFAULT".length).trim()
        if (rest.startsWith("(")) {
            val end = rest.indexOf(')')
            if (end > 0) rest = rest.substring(1, end).trim()
        }
        if (rest.startsWith("'") || rest.startsWith("\"")) {
            val q = rest.first()
            val end = rest.indexOf(q, 1)
            if (end > 0) return rest.substring(1, end)
        }
        return rest.split(Regex("\\s+")).firstOrNull()?.trim('(', ')', ';', ',')
    }

    private fun mapSqlTypeToKotlin(sqlTypeRaw: String): Pair<String, Int?> {
        val paren = sqlTypeRaw.indexOf('(')
        val base = if (paren > 0) {
            sqlTypeRaw.substring(0, paren).trim()
        } else {
            sqlTypeRaw.trim()
        }.uppercase()
        val length = if (paren > 0) {
            sqlTypeRaw.substring(paren + 1).substringBefore(')').toIntOrNull()
        } else {
            null
        }
        val kt = when {
            base == "BIGINT" || base == "BIGSERIAL" -> "Long"
            base == "INT" || base == "INTEGER" || base == "SMALLINT" || base == "MEDIUMINT" ||
                base == "SERIAL" -> "Int"
            base == "TINYINT" && length == 1 -> "Boolean"
            base == "TINYINT" -> "Int"
            base == "DOUBLE" || base == "FLOAT" || base == "REAL" -> "Double"
            base == "BOOLEAN" || base == "BOOL" -> "Boolean"
            base == "TEXT" || base.startsWith("VARCHAR") || base.startsWith("CHAR") ||
                base == "JSON" || base == "BLOB" -> "String"
            else -> "String"
        }
        return kt to length
    }
}
