/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

/**
 * Preserves DB use case registrations when merging [GeneratedDomainModule] (Android `main` tree).
 */
object AndroidGeneratedDomainModuleIo {

    private val dbBlockPattern = Regex(
        """//\s*egs-gen:db-usecases-begin\s*\n([\s\S]*?)\s*//\s*egs-gen:db-usecases-end""",
        RegexOption.MULTILINE,
    )

    private val singleOfPattern = Regex("""singleOf\s*\(\s*::\s*(\w+)\s*\)""")

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
}
