/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming

internal object KmpPreferencesKotlinEmitter {

    /** `userId` / `user_id` ? `UserId` for `getUserId` / `observeUserId`. */
    fun kotlinPropertyToPascal(name: String): String =
        name.split('_').filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }


    fun upperConstFromLogicalKey(logicalKey: String): String =
        logicalKey.split('-', '_', '.')
            .filter { it.isNotBlank() }
            .joinToString("_") { it.uppercase() }
            .ifEmpty { "KEY" }

    fun defaultLiteral(field: PrefsParsedField): String = when (field.storageKind) {
        PrefsStorageKind.STRING -> "\"\""
        PrefsStorageKind.BOOLEAN -> "false"
        PrefsStorageKind.INT -> "0"
        PrefsStorageKind.LONG -> "0L"
    }

    fun storageStringForScalarField(fieldName: String): String = fieldName

    fun scalarPrefsKeysLines(logicalKey: String, field: PrefsParsedField): String {
        val upper = upperConstFromLogicalKey(logicalKey)
        val storage = storageStringForScalarField(field.name)
        val defConst = "${upper}_DEFAULT"
        return """
            const val $upper = "$storage"
            const val $defConst = ${defaultLiteral(field)}
        """.trimIndent()
    }

    fun snapshotPrefsKeyLine(logicalKey: String): String {
        val upper = upperConstFromLogicalKey(logicalKey)
        val storage = SqlNaming.snakeToLowerCamel(logicalKey.replace('-', '_'))
        return """const val $upper = "$storage""""
    }

    fun scalarDataSourceMethods(
        modulePrefsKeysObject: String,
        logicalKey: String,
        field: PrefsParsedField,
    ): String {
        val upper = upperConstFromLogicalKey(logicalKey)
        val def = "${upper}_DEFAULT"
        val pascal = kotlinPropertyToPascal(field.name)
        val getter = "get$pascal"
        val setter = "set$pascal"
        val observer = "observe$pascal"
        val keysRef = "$modulePrefsKeysObject.Scalar"
        return when (field.storageKind) {
            PrefsStorageKind.STRING -> """
                suspend fun $getter(): ${field.kotlinType} =
                    store.getString($keysRef.$upper, $keysRef.$def)

                suspend fun $setter(value: ${field.kotlinType}) {
                    store.putString($keysRef.$upper, value)
                }

                fun $observer(): Flow<${field.kotlinType}> =
                    store.observeString($keysRef.$upper, $keysRef.$def)
            """.trimIndent()

            PrefsStorageKind.BOOLEAN -> """
                suspend fun $getter(): ${field.kotlinType} =
                    store.getBoolean($keysRef.$upper, $keysRef.$def)

                suspend fun $setter(value: ${field.kotlinType}) {
                    store.putBoolean($keysRef.$upper, value)
                }

                fun $observer(): Flow<${field.kotlinType}> =
                    store.observeBoolean($keysRef.$upper, $keysRef.$def)
            """.trimIndent()

            PrefsStorageKind.INT -> """
                suspend fun $getter(): ${field.kotlinType} =
                    store.getInt($keysRef.$upper, $keysRef.$def)

                suspend fun $setter(value: ${field.kotlinType}) {
                    store.putInt($keysRef.$upper, value)
                }

                fun $observer(): Flow<${field.kotlinType}> =
                    store.observeInt($keysRef.$upper, $keysRef.$def)
            """.trimIndent()

            PrefsStorageKind.LONG -> """
                suspend fun $getter(): ${field.kotlinType} =
                    store.getLong($keysRef.$upper, $keysRef.$def)

                suspend fun $setter(value: ${field.kotlinType}) {
                    store.putLong($keysRef.$upper, value)
                }

                fun $observer(): Flow<${field.kotlinType}> =
                    store.observeLong($keysRef.$upper, $keysRef.$def)
            """.trimIndent()
        }
    }

    fun snapshotDataSourceMethods(
        modulePrefsKeysObject: String,
        snapshotClassSimple: String,
        logicalKey: String,
    ): String {
        val upper = upperConstFromLogicalKey(logicalKey)
        val getter = "get$snapshotClassSimple"
        val setter = "set$snapshotClassSimple"
        val observer = "observe$snapshotClassSimple"
        return """
            suspend fun $getter(): $snapshotClassSimple =
                store.get(
                    $modulePrefsKeysObject.Snapshot.$upper,
                    $snapshotClassSimple.serializer(),
                    $snapshotClassSimple(),
                )

            suspend fun $setter(value: $snapshotClassSimple) {
                store.put($modulePrefsKeysObject.Snapshot.$upper, value, $snapshotClassSimple.serializer())
            }

            fun $observer(): Flow<$snapshotClassSimple> =
                store.observe(
                    $modulePrefsKeysObject.Snapshot.$upper,
                    $snapshotClassSimple.serializer(),
                    $snapshotClassSimple(),
                )
        """.trimIndent()
    }

    fun snapshotSerializableModel(className: String, fields: List<PrefsParsedField>): String =
        buildString {
            appendLine("    @kotlinx.serialization.Serializable")
            appendLine("    data class $className(")
            fields.forEachIndexed { index, f ->
                val def = defaultLiteral(f)
                val comma = if (index < fields.lastIndex) "," else ""
                appendLine("        val ${f.name}: ${f.kotlinType} = $def$comma")
            }
            appendLine("    )")
        }.trimEnd()

    fun scalarRepositoryMethods(field: PrefsParsedField): String {
        val pascal = kotlinPropertyToPascal(field.name)
        val getter = "get$pascal"
        val setter = "set$pascal"
        val observer = "observe$pascal"
        return """
            suspend fun $getter(): ${field.kotlinType}

            suspend fun $setter(value: ${field.kotlinType})

            fun $observer(): Flow<${field.kotlinType}>
        """.trimIndent()
    }

    fun snapshotRepositoryMethods(snapshotClassSimple: String): String {
        val getter = "get$snapshotClassSimple"
        val setter = "set$snapshotClassSimple"
        val observer = "observe$snapshotClassSimple"
        return """
            suspend fun $getter(): $snapshotClassSimple

            suspend fun $setter(value: $snapshotClassSimple)

            fun $observer(): Flow<$snapshotClassSimple>
        """.trimIndent()
    }

    fun scalarSupportDelegates(field: PrefsParsedField, dataSourceClass: String): String {
        val pascal = kotlinPropertyToPascal(field.name)
        val getter = "get$pascal"
        val setter = "set$pascal"
        val observer = "observe$pascal"
        return """
            override suspend fun $getter() = prefs.$getter()

            override suspend fun $setter(value: ${field.kotlinType}) = prefs.$setter(value)

            override fun $observer() = prefs.$observer()
        """.trimIndent()
    }

    fun snapshotSupportDelegates(snapshotClassSimple: String, dataSourceClass: String): String {
        val getter = "get$snapshotClassSimple"
        val setter = "set$snapshotClassSimple"
        val observer = "observe$snapshotClassSimple"
        return """
            override suspend fun $getter() = prefs.$getter()

            override suspend fun $setter(value: $snapshotClassSimple) = prefs.$setter(value)

            override fun $observer() = prefs.$observer()
        """.trimIndent()
    }
}
