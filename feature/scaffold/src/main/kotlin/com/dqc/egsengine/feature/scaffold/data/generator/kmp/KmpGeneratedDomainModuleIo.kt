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

    private val dbBlockPattern = Regex(
        """//\s*egs-gen:db-usecases-begin\s*\n([\s\S]*?)\n\s*//\s*egs-gen:db-usecases-end""",
        RegexOption.MULTILINE,
    )

    private val prefsBlockPattern = Regex(
        """//\s*egs-gen:prefs-usecases-begin\s*\n([\s\S]*?)\n\s*//\s*egs-gen:prefs-usecases-end""",
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
        val file = subProjectRoot.resolve(
            "feature/$moduleName/src/$kotlinSourceSet/kotlin/$pkgPath/generate/di/GeneratedDomainModule.kt",
        )
        if (!file.exists()) return emptyList()
        val text = file.readText()
        val block = dbBlockPattern.find(text)?.groupValues?.get(1) ?: return emptyList()
        return singleOfPattern.findAll(block).map { it.groupValues[1] }.toList()
    }

    private val dataModuleDbPattern = Regex(
        """//\s*egs-gen:database-begin\s*\n([\s\S]*?)\n\s*//\s*egs-gen:database-end""",
        RegexOption.MULTILINE,
    )

    private val dataModulePrefsPattern = Regex(
        """//\s*egs-gen:prefs-begin\s*\n([\s\S]*?)\n\s*//\s*egs-gen:prefs-end""",
        RegexOption.MULTILINE,
    )

    /**
     * When API sync runs after `gen database`, keep Room/DAO/DataSource bindings inside [GeneratedDataModule].
     * When API sync runs after `gen prefs`, keep prefs Koin bindings.
     */
    fun mergeGeneratedDataModulePreservingDatabaseBlock(
        existingContent: String?,
        generatedContent: String,
    ): String {
        val existing = existingContent ?: return generatedContent
        var result = generatedContent
        val existingDb = dataModuleDbPattern.find(existing)?.groupValues?.get(1)?.trim()
        if (!existingDb.isNullOrBlank()) {
            result = dataModuleDbPattern.replace(result) {
                "    // egs-gen:database-begin\n$existingDb\n    // egs-gen:database-end"
            }
        }
        val existingPrefs = dataModulePrefsPattern.find(existing)?.groupValues?.get(1)?.trim()
        if (!existingPrefs.isNullOrBlank()) {
            result = dataModulePrefsPattern.replace(result) {
                "    // egs-gen:prefs-begin\n$existingPrefs\n    // egs-gen:prefs-end"
            }
        }
        return result
    }

    fun replaceDbUseCasesBlock(
        existingContent: String,
        dbUseCaseClassNames: List<String>,
    ): String {
        val body = buildString {
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
        val file = subProjectRoot.resolve(
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
        val body = buildString {
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
