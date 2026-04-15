/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

/**
 * Incremental prefs codegen: generated regions are delimited by these markers (same idea as `egs-gen:database-begin`).
 */
internal object KmpPreferencesBlockMerger {

    const val SCALAR_KEYS_BEGIN = "// egs-gen:prefs-scalar-keys-begin"
    const val SCALAR_KEYS_END = "// egs-gen:prefs-scalar-keys-end"

    const val SNAPSHOT_KEYS_BEGIN = "// egs-gen:prefs-snapshot-keys-begin"
    const val SNAPSHOT_KEYS_END = "// egs-gen:prefs-snapshot-keys-end"

    const val SCALAR_DATASOURCE_BEGIN = "// egs-gen:prefs-scalar-datasource-begin"
    const val SCALAR_DATASOURCE_END = "// egs-gen:prefs-scalar-datasource-end"

    const val SNAPSHOT_DATASOURCE_BEGIN = "// egs-gen:prefs-snapshot-datasource-begin"
    const val SNAPSHOT_DATASOURCE_END = "// egs-gen:prefs-snapshot-datasource-end"

    const val REPOSITORY_BEGIN = "// egs-gen:prefs-repository-begin"
    const val REPOSITORY_END = "// egs-gen:prefs-repository-end"

    const val SUPPORT_BEGIN = "// egs-gen:prefs-support-begin"
    const val SUPPORT_END = "// egs-gen:prefs-support-end"

    fun appendToMarkedBlock(
        fileText: String,
        beginMarker: String,
        endMarker: String,
        newInnerChunk: String,
        shouldSkipAppend: (existingBlock: String) -> Boolean,
    ): String {
        val pattern = Regex(
            """([ \t]*${Regex.escape(beginMarker)}\s*\n)([\s\S]*?)(\n[ \t]*${Regex.escape(endMarker)})""",
            RegexOption.MULTILINE,
        )
        val m = pattern.find(fileText) ?: return fileText
        val inner = m.groupValues[2]
        if (shouldSkipAppend(inner)) {
            return fileText
        }
        val addition = if (inner.isBlank()) {
            newInnerChunk.trimEnd()
        } else {
            inner.trimEnd() + "\n\n" + newInnerChunk.trimEnd()
        }
        return pattern.replace(fileText) { g ->
            g.groupValues[1] + addition + g.groupValues[3]
        }
    }

    fun hasConstName(text: String, constName: String): Boolean =
        Regex("""\bconst\s+val\s+$constName\b""").containsMatchIn(text)
}
