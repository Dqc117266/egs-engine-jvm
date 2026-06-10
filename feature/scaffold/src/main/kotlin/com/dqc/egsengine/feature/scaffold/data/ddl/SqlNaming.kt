/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.ddl

/** SQL `snake_case` identifiers to Kotlin names. */
object SqlNaming {
    fun snakeToPascal(name: String): String {
        val parts = name.split('_').filter { it.isNotBlank() }
        if (parts.isEmpty()) return name.replaceFirstChar { it.uppercase() }
        return parts.joinToString("") { p -> p.replaceFirstChar { it.uppercase() } }
    }

    fun snakeToLowerCamel(name: String): String {
        val pascal = snakeToPascal(name)
        if (pascal.isEmpty()) return name
        return pascal.replaceFirstChar { it.lowercase() }
    }

    fun moduleNameToPascal(moduleName: String): String {
        val parts = moduleName.replace('-', '_').split('_').filter { it.isNotBlank() }
        return parts.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    /**
     * Last path segment of a table name in snake form, after stripping a leading `feature_` prefix
     * (e.g. `feature_demos` → `demos`, `orders` → `orders`).
     */
    fun tableNameTail(tableName: String): String {
        val withoutQuotes = tableName.trim().trim('`', '"')
        val trimmed =
            if (withoutQuotes.lowercase().startsWith("feature_")) {
                withoutQuotes.drop("feature_".length)
            } else {
                withoutQuotes
            }
        val snake = trimmed.trim('_').lowercase()
        if (snake.isEmpty()) return "items"
        val parts = snake.split('_').filter { it.isNotBlank() }
        return parts.lastOrNull() ?: "items"
    }

    /** Rough English singular for the last table segment: `demos` → `demo`, `categories` → `category`. */
    fun singularizeTableTail(tail: String): String {
        val s = tail.lowercase()
        if (s.length <= 1) return s
        if (s.endsWith("ies") && s.length > 3) return s.dropLast(3) + "y"
        if ((s.endsWith("ses") || s.endsWith("xes") || s.endsWith("ches") || s.endsWith("shes"))) {
            return s.dropLast(2)
        }
        if (s.endsWith('s')) return s.dropLast(1)
        return s
    }

    /**
     * PascalCase entity base name derived from table (e.g. `feature_demos` → `Demo`, `feature_order_items` → `OrderItem`).
     * Uses the last `_` segment, singularized, then Pascal-cased; multi-word tails become `OrderItem` style.
     */
    fun tableToEntityPascal(tableName: String): String {
        val withoutQuotes = tableName.trim().trim('`', '"')
        val trimmed =
            if (withoutQuotes.lowercase().startsWith("feature_")) {
                withoutQuotes.drop("feature_".length)
            } else {
                withoutQuotes
            }
        val snake = trimmed.trim('_').lowercase()
        if (snake.isEmpty()) return "Item"
        val parts = snake.split('_').filter { it.isNotBlank() }
        if (parts.isEmpty()) return "Item"
        return parts.joinToString("") { p ->
            snakeToPascal(singularizeTableTail(p))
        }
    }

    /**
     * REST plural segment under `/api/<something>/`, e.g. `feature_demos` → `demos`.
     * If the tail already looks plural (ends with `s`), it is returned as lowercase; otherwise pluralised.
     */
    fun tableToRestPath(tableName: String): String {
        val last = tableNameTail(tableName)
        val lower = last.lowercase()
        if (lower.isEmpty()) return "items"
        if (lower.last() == 's' && lower.length > 2) return lower
        return pluralTail(lower)
    }

    private fun pluralTail(s: String): String = when {
        s.endsWith("s") || s.endsWith("x") || s.endsWith("ch") || s.endsWith("sh") -> "${s}es"
        s.endsWith("y") &&
            s.length > 1 &&
            s[s.length - 2].isLetter() &&
            s[s.length - 2].lowercaseChar() !in listOf('a', 'e', 'i', 'o', 'u')
        -> s.dropLast(1) + "ies"

        else -> "${s}s"
    }
}
