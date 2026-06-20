package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityTemplateModel

/**
 * Rewrites ONLY the `# === EGS-AUTOGEN-BEGIN ===` … `# === EGS-AUTOGEN-END ===`
 * region of `autoload/EntityRegistry.gd`, inserting one `preload()` const per
 * generated entity. Content outside the markers is never modified.
 *
 * Contract (`egs-godot-template/docs/GENERATOR.md`):
 *  - new const line: `const <Class> = preload("res://entities/<dir>/<snake>.gd")`
 *  - duplicate `class_name` already present in the region => error, no overwrite
 *  - the region keeps a leading placeholder comment so it is never empty
 */
class GodotEntityRegistryUpdater {
    fun addEntity(
        registryContent: String,
        model: GodotEntityTemplateModel,
    ): RegistryUpdate {
        requireMarkers(registryContent)

        // Reject duplicates against the *whole* file (existing consts anywhere),
        // so a hand-written const outside the markers also blocks a clash.
        if (hasConstFor(registryContent, model.className)) {
            error(
                "EntityRegistry.gd already defines a const named '${model.className}'. " +
                    "Pick a different name or remove the existing entry first.",
            )
        }

        val updated = insertIntoRegion(registryContent, buildConstLine(model))
        return RegistryUpdate(
            before = registryContent,
            after = updated,
            addedLine = buildConstLine(model),
        )
    }

    /** True when the registry text already declares `const <className>`. */
    fun hasConstFor(
        registryContent: String,
        className: String,
    ): Boolean = hasConstForInternal(registryContent, className)

    private fun requireMarkers(content: String) {
        require(BEGIN_REGEX.containsMatchIn(content)) {
            "EntityRegistry.gd is missing the '# === EGS-AUTOGEN-BEGIN ===' marker. " +
                "Run the egs-godot-template that ships the registry scaffold, or re-add the marker block."
        }
        require(END_REGEX.containsMatchIn(content)) {
            "EntityRegistry.gd is missing the '# === EGS-AUTOGEN-END ===' marker."
        }
    }

    private fun insertIntoRegion(
        content: String,
        constLine: String,
    ): String {
        val beginIdx = content.indexOf(BEGIN_MARKER)
        val endIdx = content.indexOf(END_MARKER)
        require(beginIdx in 0..<endIdx) { "EGS markers out of order in EntityRegistry.gd" }

        // `afterBegin` indexes the newline that terminates the BEGIN marker line;
        // `beforeEnd` indexes the start of the END marker line.
        val afterBegin = content.indexOf('\n', beginIdx).let { if (it < 0) content.length else it }
        val beforeEnd = endIdx
        val regionBody = content.substring(afterBegin + 1, beforeEnd)

        val existingLines =
            regionBody
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != PLACEHOLDER }
                .toMutableList()
        existingLines += constLine

        // Rebuild ONLY the body between the markers. The BEGIN marker (with its newline)
        // is already retained by `prefix`; the END marker is retained by `suffix`.
        val rebuiltBody =
            if (existingLines.isEmpty()) {
                PLACEHOLDER
            } else {
                existingLines.joinToString("\n")
            }

        val prefix = content.substring(0, afterBegin + 1) // up to & incl. BEGIN marker's newline
        val suffix = content.substring(beforeEnd) // from the END marker onward
        return "$prefix$rebuiltBody\n$suffix"
    }

    private fun buildConstLine(model: GodotEntityTemplateModel): String = "const ${model.className} = preload(\"${model.generatedScriptResPath}\")"

    private fun hasConstForInternal(
        content: String,
        className: String,
    ): Boolean {
        val pattern = Regex("""const\s+\Q$className\E\b""")
        return pattern.containsMatchIn(content)
    }

    data class RegistryUpdate(
        val before: String,
        val after: String,
        val addedLine: String,
    )

    companion object {
        const val BEGIN_MARKER = "# === EGS-AUTOGEN-BEGIN ==="
        const val END_MARKER = "# === EGS-AUTOGEN-END ==="
        const val PLACEHOLDER = "# (egs game add inserts preload() consts here)"

        private val BEGIN_REGEX = Regex(Regex.escape(BEGIN_MARKER))
        private val END_REGEX = Regex(Regex.escape(END_MARKER))
    }
}
