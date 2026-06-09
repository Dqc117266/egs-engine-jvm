/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

import org.slf4j.LoggerFactory
import java.io.File

/**
 * Shared file merge/create logic for prefs scaffolding.
 * Used by both [KmpPreferencesScaffolder] and [AndroidPreferencesScaffolder].
 */
object PrefsFileMerger {
    private val logger = LoggerFactory.getLogger(PrefsFileMerger::class.java)

    fun renderSnapshotModelFile(modelPkg: String, className: String, body: String): String =
        """
        /*
         * Copyright 2026 Mifos Initiative
         *
         * SPDX-License-Identifier: MPL-2.0
         */
        package $modelPkg

        ${body.trim()}
        """.trimIndent() + "\n"

    fun indentPrefsKeysBlock(text: String): String = text.prependIndent("        ")

    fun indentClassMemberBlock(text: String): String = text.trimEnd().prependIndent("    ")

    fun mergeOrCreatePrefsKeys(
        file: File,
        prefsKeysObject: String,
        modelPkg: String,
        mode: PrefsGenerationMode,
        force: Boolean,
    ): String {
        return when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val upper = KmpPreferencesKotlinEmitter.upperConstFromLogicalKey(mode.logicalKey)
                val chunk = KmpPreferencesKotlinEmitter.scalarPrefsKeysLines(mode.logicalKey, mode.field)
                if (!file.exists()) {
                    renderPrefsKeysFull(modelPkg, prefsKeysObject, scalarInner = chunk, snapshotInner = "")
                } else {
                    var text = file.readText()
                    if (KmpPreferencesBlockMerger.hasConstName(text, upper)) {
                        if (force) {
                            text = replaceScalarKeyConst(text, upper, chunk)
                        } else {
                            logger.info("Scalar key {} already present in {}; skipping keys merge", upper, file.path)
                            return text
                        }
                    } else {
                        text = KmpPreferencesBlockMerger.appendToMarkedBlock(
                            text,
                            KmpPreferencesBlockMerger.SCALAR_KEYS_BEGIN,
                            KmpPreferencesBlockMerger.SCALAR_KEYS_END,
                            indentPrefsKeysBlock(chunk),
                            shouldSkipAppend = { false },
                        )
                    }
                    text
                }
            }
            is PrefsGenerationMode.Snapshot -> {
                val upper = KmpPreferencesKotlinEmitter.upperConstFromLogicalKey(mode.logicalKey)
                val chunk = KmpPreferencesKotlinEmitter.snapshotPrefsKeyLine(mode.logicalKey)
                if (!file.exists()) {
                    renderPrefsKeysFull(modelPkg, prefsKeysObject, scalarInner = "", snapshotInner = chunk)
                } else {
                    var text = file.readText()
                    if (KmpPreferencesBlockMerger.hasConstName(text, upper)) {
                        if (force) {
                            text = replaceSnapshotKeyLine(text, upper, chunk)
                        } else {
                            throw IllegalStateException(
                                "Snapshot key const $upper already exists in ${file.name}. Use --force to replace, " +
                                    "or choose another --key.",
                            )
                        }
                    } else {
                        text = KmpPreferencesBlockMerger.appendToMarkedBlock(
                            text,
                            KmpPreferencesBlockMerger.SNAPSHOT_KEYS_BEGIN,
                            KmpPreferencesBlockMerger.SNAPSHOT_KEYS_END,
                            indentPrefsKeysBlock(chunk),
                            shouldSkipAppend = { false },
                        )
                    }
                    text
                }
            }
        }
    }

    fun replaceScalarKeyConst(text: String, upper: String, newChunk: String): String {
        val pattern = Regex(
            """const val $upper = \"[^\"]*\"\s*\n\s*const val ${upper}_DEFAULT = [^\n]+""",
            RegexOption.MULTILINE,
        )
        return pattern.replace(text) {
            newChunk.prependIndent("        ")
        }
    }

    fun replaceSnapshotKeyLine(text: String, upper: String, newLine: String): String {
        val pattern = Regex("""const val $upper = \"[^\"]*\"""")
        return pattern.replace(text, newLine.prependIndent("        "))
    }

    fun renderPrefsKeysFull(
        modelPkg: String,
        prefsKeysObject: String,
        scalarInner: String,
        snapshotInner: String,
    ): String {
        val scalarBlock = if (scalarInner.isBlank()) "" else indentPrefsKeysBlock(scalarInner)
        val snapshotBlock = if (snapshotInner.isBlank()) "" else indentPrefsKeysBlock(snapshotInner)
        return buildString {
            appendLine("/*")
            appendLine(" * Copyright 2026 Mifos Initiative")
            appendLine(" *")
            appendLine(" * SPDX-License-Identifier: MPL-2.0")
            appendLine(" */")
            appendLine("package $modelPkg")
            appendLine()
            appendLine("internal object $prefsKeysObject {")
            appendLine("    object Scalar {")
            appendLine("        ${KmpPreferencesBlockMerger.SCALAR_KEYS_BEGIN}")
            if (scalarBlock.isNotBlank()) {
                appendLine(scalarBlock)
            }
            appendLine("        ${KmpPreferencesBlockMerger.SCALAR_KEYS_END}")
            appendLine("    }")
            appendLine()
            appendLine("    object Snapshot {")
            appendLine("        ${KmpPreferencesBlockMerger.SNAPSHOT_KEYS_BEGIN}")
            if (snapshotBlock.isNotBlank()) {
                appendLine(snapshotBlock)
            }
            appendLine("        ${KmpPreferencesBlockMerger.SNAPSHOT_KEYS_END}")
            appendLine("    }")
            appendLine("}")
        }.trimEnd() + "\n"
    }

    @Suppress("LongParameterList")
    fun mergeOrCreateDataSource(
        existingFile: File,
        prefsPkg: String,
        modelPkg: String,
        dataSourceClass: String,
        prefsKeysObject: String,
        prefsKeysFq: String,
        mode: PrefsGenerationMode,
        force: Boolean,
        storeImport: String,
    ): String {
        val keysImport = "import $prefsKeysFq"
        val flowImport = "import kotlinx.coroutines.flow.Flow"

        val chunk = when (mode) {
            is PrefsGenerationMode.Scalar ->
                KmpPreferencesKotlinEmitter.scalarDataSourceMethods(prefsKeysObject, mode.logicalKey, mode.field)
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesKotlinEmitter.snapshotDataSourceMethods(
                    prefsKeysObject,
                    mode.snapshotClassName,
                    mode.logicalKey,
                )
        }

        if (!existingFile.exists()) {
            val snapImport = if (mode is PrefsGenerationMode.Snapshot) {
                "import $modelPkg.${mode.snapshotClassName}"
            } else {
                ""
            }
            val scalarBody = if (mode is PrefsGenerationMode.Scalar) chunk else ""
            val snapshotBody = if (mode is PrefsGenerationMode.Snapshot) chunk else ""
            val sortedImports = buildList {
                add(keysImport)
                add(flowImport)
                if (snapImport.isNotBlank()) add(snapImport)
                add(storeImport)
            }.sorted()
            return buildString {
                appendLine("/*")
                appendLine(" * Copyright 2026 Mifos Initiative")
                appendLine(" *")
                appendLine(" * SPDX-License-Identifier: MPL-2.0")
                appendLine(" */")
                appendLine("package $prefsPkg")
                appendLine()
                sortedImports.forEach { appendLine(it) }
                appendLine()
                appendLine("internal class $dataSourceClass(")
                appendLine("    private val store: TypedPreferenceStore,")
                appendLine(") {")
                appendLine()
                appendLine("    ${KmpPreferencesBlockMerger.SCALAR_DATASOURCE_BEGIN}")
                if (scalarBody.isNotBlank()) {
                    append(indentClassMemberBlock(scalarBody))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.SCALAR_DATASOURCE_END}")
                appendLine()
                appendLine("    ${KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_BEGIN}")
                if (snapshotBody.isNotBlank()) {
                    append(indentClassMemberBlock(snapshotBody))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_END}")
                appendLine("}")
            }.trimEnd() + "\n"
        }

        var text = existingFile.readText()
        val markerPair = when (mode) {
            is PrefsGenerationMode.Scalar ->
                KmpPreferencesBlockMerger.SCALAR_DATASOURCE_BEGIN to KmpPreferencesBlockMerger.SCALAR_DATASOURCE_END
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_BEGIN to KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_END
        }
        val methodPrefix = when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val p = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                "suspend fun get$p"
            }
            is PrefsGenerationMode.Snapshot -> "suspend fun get${mode.snapshotClassName}"
        }
        if (text.contains(methodPrefix) && !force) {
            logger.info("DataSource already contains {}; skipping datasource merge", methodPrefix)
            return text
        }
        if (mode is PrefsGenerationMode.Snapshot) {
            val snapImportLine = "import $modelPkg.${mode.snapshotClassName}"
            if (!text.contains(snapImportLine)) {
                text = insertImportAfterPackage(text, snapImportLine)
            }
        }
        text = KmpPreferencesBlockMerger.appendToMarkedBlock(
            text,
            markerPair.first,
            markerPair.second,
            indentClassMemberBlock(chunk),
            shouldSkipAppend = { inner -> inner.contains(methodPrefix) },
        )
        if (!text.contains(keysImport)) {
            text = insertImportAfterPackage(text, keysImport)
        }
        if (!text.contains(storeImport)) {
            text = insertImportAfterPackage(text, storeImport)
        }
        if (!text.contains(flowImport)) {
            text = insertImportAfterPackage(text, flowImport)
        }
        return text
    }

    fun insertImportAfterPackage(text: String, importLine: String): String {
        val pkg = Regex("^package\\s+\\S+", RegexOption.MULTILINE).find(text) ?: return importLine + "\n" + text
        val insertAt = pkg.range.last + 1
        return text.substring(0, insertAt) + "\n" + importLine + text.substring(insertAt)
    }

    fun mergeOrCreatePrefsRepository(
        file: File,
        domainRepoPkg: String,
        prefsRepoName: String,
        mode: PrefsGenerationMode,
        modelPkg: String = "",
        includeSnapshotImport: Boolean = true,
    ): String {
        val chunk = when (mode) {
            is PrefsGenerationMode.Scalar -> KmpPreferencesKotlinEmitter.scalarRepositoryMethods(mode.field)
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesKotlinEmitter.snapshotRepositoryMethods(mode.snapshotClassName)
        }
        if (!file.exists()) {
            return buildString {
                appendLine("/*")
                appendLine(" * Copyright 2026 Mifos Initiative")
                appendLine(" *")
                appendLine(" * SPDX-License-Identifier: MPL-2.0")
                appendLine(" */")
                appendLine("package $domainRepoPkg")
                appendLine()
                appendLine("import kotlinx.coroutines.flow.Flow")
                if (includeSnapshotImport && mode is PrefsGenerationMode.Snapshot && modelPkg.isNotBlank()) {
                    appendLine("import $modelPkg.${mode.snapshotClassName}")
                }
                appendLine()
                appendLine("internal interface $prefsRepoName {")
                appendLine("    ${KmpPreferencesBlockMerger.REPOSITORY_BEGIN}")
                if (chunk.isNotBlank()) {
                    append(indentClassMemberBlock(chunk))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.REPOSITORY_END}")
                appendLine("}")
            }.trimEnd() + "\n"
        }
        val text = file.readText()
        val sig = when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val p = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                "suspend fun get$p"
            }
            is PrefsGenerationMode.Snapshot -> "suspend fun get${mode.snapshotClassName}"
        }
        return KmpPreferencesBlockMerger.appendToMarkedBlock(
            text,
            KmpPreferencesBlockMerger.REPOSITORY_BEGIN,
            KmpPreferencesBlockMerger.REPOSITORY_END,
            indentClassMemberBlock(chunk),
            shouldSkipAppend = { inner -> inner.contains(sig) },
        )
    }

    fun mergeOrCreatePrefsSupport(
        file: File,
        dataRepoPkg: String,
        domainRepoPkg: String,
        prefsSupportName: String,
        prefsRepoName: String,
        dataSourceClass: String,
        prefsPkg: String,
        mode: PrefsGenerationMode,
        modelPkg: String = "",
        includeSnapshotImport: Boolean = true,
    ): String {
        val chunk = when (mode) {
            is PrefsGenerationMode.Scalar ->
                KmpPreferencesKotlinEmitter.scalarSupportDelegates(mode.field, dataSourceClass)
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesKotlinEmitter.snapshotSupportDelegates(mode.snapshotClassName, dataSourceClass)
        }
        if (!file.exists()) {
            return buildString {
                appendLine("/*")
                appendLine(" * Copyright 2026 Mifos Initiative")
                appendLine(" *")
                appendLine(" * SPDX-License-Identifier: MPL-2.0")
                appendLine(" */")
                appendLine("package $dataRepoPkg")
                appendLine()
                appendLine("import $prefsPkg.$dataSourceClass")
                appendLine("import $domainRepoPkg.$prefsRepoName")
                if (includeSnapshotImport && mode is PrefsGenerationMode.Snapshot && modelPkg.isNotBlank()) {
                    appendLine("import $modelPkg.${mode.snapshotClassName}")
                }
                appendLine()
                appendLine("internal class $prefsSupportName(")
                appendLine("    private val prefs: $dataSourceClass,")
                appendLine(") : $prefsRepoName {")
                appendLine("    ${KmpPreferencesBlockMerger.SUPPORT_BEGIN}")
                if (chunk.isNotBlank()) {
                    append(indentClassMemberBlock(chunk))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.SUPPORT_END}")
                appendLine("}")
            }.trimEnd() + "\n"
        }
        val text = file.readText()
        val sig = when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val p = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                "override suspend fun get$p"
            }
            is PrefsGenerationMode.Snapshot -> "override suspend fun get${mode.snapshotClassName}"
        }
        return KmpPreferencesBlockMerger.appendToMarkedBlock(
            text,
            KmpPreferencesBlockMerger.SUPPORT_BEGIN,
            KmpPreferencesBlockMerger.SUPPORT_END,
            indentClassMemberBlock(chunk),
            shouldSkipAppend = { inner -> inner.contains(sig) },
        )
    }
}
