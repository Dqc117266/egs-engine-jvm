package com.dqc.egsengine.feature.scaffold.data.generator.godot

import java.io.File

/**
 * Manages a module's `generated/registry.gd`: creates it from a standard template when
 * missing, and rewrites ONLY the `# === EGS-AUTOGEN-BEGIN ===`…`END` region with sorted,
 * unique `preload()` const lines. Content outside the markers is never modified, and the
 * global `app/autoload/EntityRegistry.gd` aggregator is never touched by this updater.
 *
 * Mirrors the Python tool's `registry_add` / `REGISTRY_TEMPLATE`.
 *
 * @param contract the project's generator.json contract (markers + lineFormat + relativePath).
 */
class GodotModuleRegistryUpdater(private val contract: GodotRegistryContract) {
    private val lineRegex = Regex("""^\s*const\s+(\w+)\s*=\s*preload\("res://([^"]+)"\)""")

    /**
     * Add `const [className] = preload("[scriptRes]")` to the module's registry.
     *
     * @param moduleRoot the project root (registry path is `modules/[module]/<relativePath>`).
     * @param module the module name (e.g. `combat`).
     * @param className PascalCase class registered (e.g. `Slime`).
     * @param scriptRes `res://` path of the registered Generated script.
     * @return the registry diff; [RegistryDiff.added] is null when the const was already present
     *   (idempotent — never an error, matching the Python tool).
     */
    fun addConst(
        projectRoot: File,
        module: String,
        className: String,
        scriptRes: String,
    ): RegistryDiff {
        val regFile = projectRoot.resolve("modules/$module/${contract.relativePath}")
        val before =
            if (regFile.exists()) {
                regFile.readText()
            } else {
                registryTemplate(module)
            }
        val (beginIdx, endIdx, entries) = parseRegion(before)
        if (entries.any { it.className == className }) {
            // Idempotent: already present. No rewrite, no error.
            return RegistryDiff(before = before, after = before, added = null)
        }

        val newLine =
            contract.lineFormat
                .replace("{className}", className)
                .replace("{scriptPath}", scriptRes)

        val lines = before.split("\n").toMutableList()
        val body = (lines.subList(beginIdx + 1, endIdx) + newLine)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        val newLines = lines.subList(0, beginIdx + 1) + body + lines.subList(endIdx, lines.size)
        var after = newLines.joinToString("\n")
        if (!after.endsWith("\n")) after += "\n"

        return RegistryDiff(before = before, after = after, added = newLine)
    }

    /** True when the module registry already declares `const [className]`. */
    fun hasConst(
        projectRoot: File,
        module: String,
        className: String,
    ): Boolean {
        val regFile = projectRoot.resolve("modules/$module/${contract.relativePath}")
        if (!regFile.exists()) return false
        val (_, _, entries) = parseRegion(regFile.readText())
        return entries.any { it.className == className }
    }

    private fun parseRegion(text: String): Triple<Int, Int, List<RegistryEntry>> {
        val lines = text.split("\n")
        val beginIdx =
            lines.indexOfFirst { it.trim() == contract.beginMarker }
                .also { require(it >= 0) { "Registry marker '${contract.beginMarker}' not found." } }
        val endIdx =
            lines.indexOfFirst { it.trim() == contract.endMarker }
                .also { require(it > beginIdx) { "Registry marker '${contract.endMarker}' not found after BEGIN." } }
        val entries =
            lines.subList(beginIdx + 1, endIdx).mapNotNull { line ->
                lineRegex.find(line)?.let { RegistryEntry(it.groupValues[1], it.groupValues[2]) }
            }
        return Triple(beginIdx, endIdx, entries)
    }

    private fun registryTemplate(module: String): String = """extends Node
## $module generated entity registry.
##
## Managed by `egs add <entity>`. Only the EGS-AUTOGEN block is rewritten; do
## not edit between the markers. ModuleRegistry loads and instantiates this
## script; EntityRegistry reads its consts via ModuleRegistry.

${contract.beginMarker}
# (egs add inserts preload() consts here)
${contract.endMarker}
"""

    private data class RegistryEntry(val className: String, val scriptRes: String)
}

/** Result of a registry add: before/after text plus the added line (null when idempotent). */
data class RegistryDiff(
    val before: String,
    val after: String,
    val added: String?,
)
