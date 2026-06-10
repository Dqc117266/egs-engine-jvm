/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import java.io.File

/**
 * Preserves DB use case registrations when API sync regenerates [GeneratedDomainModule].
 */
object KmpGeneratedDomainModuleIo {
    /**
     * Body may be empty (FreeMarker emits begin/end markers on consecutive lines). A newline between
     * body and end marker is not guaranteed when the list is empty, so the regex allows optional
     * whitespace before the closing marker (not only a single newline).
     */
    private val dbBlockPattern =
        Regex(
            """//\s*egs-gen:db-usecases-begin\s*\n([\s\S]*?)\s*//\s*egs-gen:db-usecases-end""",
            RegexOption.MULTILINE,
        )

    private val prefsBlockPattern =
        Regex(
            """//\s*egs-gen:prefs-usecases-begin\s*\n([\s\S]*?)\s*//\s*egs-gen:prefs-usecases-end""",
            RegexOption.MULTILINE,
        )

    private val singleOfPattern = Regex("""singleOf\s*\(\s*::\s*(\w+)\s*\)""")

    fun extractDbUseCaseClassNames(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
        kotlinSourceSet: String = "commonMain",
    ): List<String> {
        val pkgPath = template.packageName.replace('.', '/')
        val file =
            subProjectRoot.resolve(
                "feature/$moduleName/src/$kotlinSourceSet/kotlin/$pkgPath/generate/di/GeneratedDomainModule.kt",
            )
        if (!file.exists()) return emptyList()
        val text = file.readText()
        val block = dbBlockPattern.find(text)?.groupValues?.get(1) ?: return emptyList()
        return singleOfPattern.findAll(block).map { it.groupValues[1] }.toList()
    }

    /**
     * Match full lines for begin/end so replace does not leave a second indent before `//`.
     * Body may be empty (consecutive markers); [RegexOption.MULTILINE] makes `^` match line starts.
     */
    private val dataModuleDbPattern =
        Regex(
            """^\s*//\s*egs-gen:database-begin\s*\n([\s\S]*?)^\s*//\s*egs-gen:database-end""",
            RegexOption.MULTILINE,
        )

    private val dataModulePrefsPattern =
        Regex(
            """^\s*//\s*egs-gen:prefs-begin\s*\n([\s\S]*?)^\s*//\s*egs-gen:prefs-end""",
            RegexOption.MULTILINE,
        )

    private val kotlinImportLineRegex =
        Regex(
            """^\s*import\s+.+$""",
            RegexOption.MULTILINE,
        )

    private fun String.trimSurroundingNewlinesOnly(): String = trimStart { it == '\n' || it == '\r' }.trimEnd { it == '\n' || it == '\r' }

    /**
     * API sync regenerates [GeneratedDataModule] with only Retrofit/API imports; merge non-API imports
     * (Room, prefs, etc.) from the existing file.
     */
    private fun mergeImportSectionFromExisting(
        existing: String,
        generated: String,
    ): String {
        val existingImports = kotlinImportLineRegex.findAll(existing).map { it.value.trim() }.toList()
        val generatedImports = kotlinImportLineRegex.findAll(generated).map { it.value.trim() }.toList()
        val generatedSet = generatedImports.toSet()
        val extras = existingImports.filter { it !in generatedSet }.distinct()
        if (extras.isEmpty()) return generated
        val merged = (generatedImports + extras).distinct().sorted()
        val lines = generated.lines().toMutableList()
        val firstImportIdx = lines.indexOfFirst { it.trimStart().startsWith("import ") }
        if (firstImportIdx < 0) {
            val pkgIdx = lines.indexOfFirst { it.startsWith("package ") }
            var insertAt = if (pkgIdx >= 0) pkgIdx + 1 else 0
            while (insertAt < lines.size && lines[insertAt].isBlank()) insertAt++
            lines.addAll(insertAt, merged)
            return lines.joinToString("\n").trimEnd() + "\n"
        }
        var lastImportIdx = firstImportIdx
        while (lastImportIdx + 1 < lines.size && lines[lastImportIdx + 1].trimStart().startsWith("import ")) {
            lastImportIdx++
        }
        val head = lines.take(firstImportIdx)
        val tail = lines.drop(lastImportIdx + 1)
        return (head + merged + tail).joinToString("\n").trimEnd() + "\n"
    }

    /**
     * When API sync runs after `gen database`, keep Room/DAO/DataSource bindings inside [GeneratedDataModule].
     * When API sync runs after `gen prefs`, keep prefs Koin bindings.
     */
    fun mergeGeneratedDataModulePreservingDatabaseBlock(
        existingContent: String?,
        generatedContent: String,
    ): String {
        val existing = existingContent ?: return generatedContent
        var result = mergeImportSectionFromExisting(existing, generatedContent)
        val existingDb =
            dataModuleDbPattern
                .find(existing)
                ?.groupValues
                ?.get(1)
                ?.trimSurroundingNewlinesOnly()
        if (!existingDb.isNullOrBlank()) {
            result =
                dataModuleDbPattern.replace(result) {
                    "    // egs-gen:database-begin\n$existingDb\n    // egs-gen:database-end"
                }
        }
        val existingPrefs =
            dataModulePrefsPattern
                .find(existing)
                ?.groupValues
                ?.get(1)
                ?.trimSurroundingNewlinesOnly()
        if (!existingPrefs.isNullOrBlank()) {
            result =
                dataModulePrefsPattern.replace(result) {
                    "    // egs-gen:prefs-begin\n$existingPrefs\n    // egs-gen:prefs-end"
                }
        }
        return result
    }

    fun replaceDbUseCasesBlock(
        existingContent: String,
        dbUseCaseClassNames: List<String>,
    ): String {
        val body =
            buildString {
                for (name in dbUseCaseClassNames) {
                    appendLine("    singleOf(::$name)")
                }
            }.trimEnd()
        val replacement = "// egs-gen:db-usecases-begin\n$body\n    // egs-gen:db-usecases-end"
        return if (dbBlockPattern.containsMatchIn(existingContent)) {
            dbBlockPattern.replace(existingContent, replacement)
        } else {
            existingContent
        }
    }

    fun extractPrefsUseCaseClassNames(
        subProjectRoot: File,
        moduleName: String,
        template: ModuleTemplate,
        kotlinSourceSet: String = "commonMain",
    ): List<String> {
        val pkgPath = template.packageName.replace('.', '/')
        val file =
            subProjectRoot.resolve(
                "feature/$moduleName/src/$kotlinSourceSet/kotlin/$pkgPath/generate/di/GeneratedDomainModule.kt",
            )
        if (!file.exists()) return emptyList()
        val text = file.readText()
        val block = prefsBlockPattern.find(text)?.groupValues?.get(1) ?: return emptyList()
        return singleOfPattern.findAll(block).map { it.groupValues[1] }.toList()
    }

    fun replacePrefsUseCasesBlock(
        existingContent: String,
        prefsUseCaseClassNames: List<String>,
    ): String {
        val body =
            buildString {
                for (name in prefsUseCaseClassNames) {
                    appendLine("    singleOf(::$name)")
                }
            }.trimEnd()
        val replacement = "// egs-gen:prefs-usecases-begin\n$body\n    // egs-gen:prefs-usecases-end"
        return if (prefsBlockPattern.containsMatchIn(existingContent)) {
            prefsBlockPattern.replace(existingContent, replacement)
        } else {
            existingContent
        }
    }
}
