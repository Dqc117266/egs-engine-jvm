/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

/**
 * Parsed `--fields` entry: Kotlin property [name] and storage scalar type.
 */
data class PrefsParsedField(
    val name: String,
    val kotlinType: String,
    val storageKind: PrefsStorageKind,
)

enum class PrefsStorageKind {
    STRING,
    BOOLEAN,
    INT,
    LONG,
}

/**
 * `userId:String, isLogin:bool` ¡ú list of fields. Whitespace allowed around `:` and `,`.
 */
object PrefsFieldParser {

    fun parseFields(fieldsArg: String): List<PrefsParsedField> {
        val trimmed = fieldsArg.trim()
        require(trimmed.isNotEmpty()) { "--fields must not be empty" }
        return trimmed.split(',').map { part ->
            val segment = part.trim()
            require(segment.isNotEmpty()) { "Empty field in --fields" }
            val colon = segment.indexOf(':')
            require(colon > 0) { "Each field must be name:type, got: $segment" }
            val name = segment.substring(0, colon).trim()
            val typeRaw = segment.substring(colon + 1).trim()
            require(name.isNotEmpty()) { "Field name missing in: $segment" }
            require(typeRaw.isNotEmpty()) { "Field type missing in: $segment" }
            val kind = normalizeType(typeRaw)
            val kt = kotlinTypeFor(kind)
            PrefsParsedField(name = name, kotlinType = kt, storageKind = kind)
        }
    }

    private fun normalizeType(raw: String): PrefsStorageKind {
        return when (raw.lowercase()) {
            "string" -> PrefsStorageKind.STRING
            "boolean", "bool" -> PrefsStorageKind.BOOLEAN
            "int" -> PrefsStorageKind.INT
            "long" -> PrefsStorageKind.LONG
            else -> throw IllegalArgumentException(
                "Unsupported preference type '$raw'. Use String, Boolean, Int, or Long.",
            )
        }
    }

    private fun kotlinTypeFor(kind: PrefsStorageKind): String = when (kind) {
        PrefsStorageKind.STRING -> "String"
        PrefsStorageKind.BOOLEAN -> "Boolean"
        PrefsStorageKind.INT -> "Int"
        PrefsStorageKind.LONG -> "Long"
    }
}
