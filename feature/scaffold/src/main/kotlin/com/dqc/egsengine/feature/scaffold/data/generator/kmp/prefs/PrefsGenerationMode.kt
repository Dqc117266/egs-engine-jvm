/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

/**
 * Scalar: one field, [logicalKey] drives const names (e.g. `user_id` ¡ú USER_ID).
 * Snapshot: two or more fields; [logicalKey] names the snapshot preference key and [snapshotClassName] the `@Serializable` model.
 */
sealed class PrefsGenerationMode {
    data class Scalar(
        val logicalKey: String,
        val field: PrefsParsedField,
    ) : PrefsGenerationMode()

    data class Snapshot(
        val logicalKey: String,
        val snapshotClassName: String,
        val fields: List<PrefsParsedField>,
    ) : PrefsGenerationMode()
}

object PrefsGenerationModeResolver {

    fun resolve(
        keyArg: String?,
        fields: List<PrefsParsedField>,
    ): PrefsGenerationMode {
        require(fields.isNotEmpty()) { "At least one field is required" }
        return when {
            fields.size == 1 -> {
                val logical = keyArg?.trim()?.takeIf { it.isNotEmpty() }
                    ?: camelToSnake(fields[0].name)
                PrefsGenerationMode.Scalar(logicalKey = logical, field = fields[0])
            }
            else -> {
                val logical = keyArg?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException(
                        "Snapshot mode (${fields.size} fields) requires --key <name> (e.g. --key config).",
                    )
                PrefsGenerationMode.Snapshot(
                    logicalKey = logical,
                    snapshotClassName = snakeOrKebabToPascal(logical),
                    fields = fields,
                )
            }
        }
    }

    private fun camelToSnake(name: String): String {
        if (name.isEmpty()) return name
        val out = StringBuilder()
        name.forEachIndexed { i, c ->
            when {
                c.isUpperCase() && i > 0 -> {
                    out.append('_')
                    out.append(c.lowercaseChar())
                }
                else -> out.append(c.lowercaseChar())
            }
        }
        return out.toString()
    }

    private fun snakeOrKebabToPascal(key: String): String =
        key.split('-', '_')
            .filter { it.isNotBlank() }
            .joinToString("") { part ->
                part.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
            .ifEmpty { "PrefsSnapshot" }
}
