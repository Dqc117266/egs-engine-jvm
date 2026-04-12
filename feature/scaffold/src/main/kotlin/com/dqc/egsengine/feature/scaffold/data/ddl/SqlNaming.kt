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
}
