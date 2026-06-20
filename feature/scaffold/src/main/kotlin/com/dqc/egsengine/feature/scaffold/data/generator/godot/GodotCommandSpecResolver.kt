package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotPathInterpolator.Names

/**
 * Maps a contract `templateId` to its output path field + file mode, and validates a
 * resolved command. Mirrors the Python tool's `_path_field_for` + eager validation.
 */
internal object GodotCommandSpecResolver {
    /**
     * Per-artifact resolution of a single templateId for a command.
     *
     * @property templateId the `.ftl` name (e.g. `generated.gd.ftl`)
     * @property relPath interpolated output path relative to the project root
     * @property mode how the file is treated for create/overwrite
     */
    data class ResolvedArtifact(
        val templateId: String,
        val relPath: String,
        val mode: GodotFileMode,
    )

    /** Resolve every templateId of [spec] into a [ResolvedArtifact], in declared order. */
    fun resolveArtifacts(
        spec: GodotCommandSpec,
        names: Names,
    ): List<ResolvedArtifact> = spec.templateIds.map { tid ->
        val (field, mode) = fieldAndMode(tid)
        val template = readField(spec, field)
            ?: error("No $field for command (template $tid). The generator.json contract is incomplete.")
        ResolvedArtifact(
            templateId = tid,
            relPath = GodotPathInterpolator.interpolate(template, names),
            mode = mode,
        )
    }

    private fun readField(
        spec: GodotCommandSpec,
        field: String,
    ): String? = when (field) {
        "generatedScriptPath" -> spec.generatedScriptPath
        "userScriptPath" -> spec.userScriptPath
        "scenePath" -> spec.scenePath
        "dataPath" -> spec.dataPath
        "moduleJsonPath" -> spec.moduleJsonPath
        "readmePath" -> spec.readmePath
        else -> null
    }

    /** templateId stem -> (contract path field, file mode). Mirrors Python `_path_field_for`. */
    private fun fieldAndMode(templateId: String): Pair<String, GodotFileMode> {
        val stem = templateId.removeSuffix(".ftl")
        return when (stem) {
            "generated.gd" -> "generatedScriptPath" to GodotFileMode.GENERATED_REBUILDABLE
            "stub.gd" -> "userScriptPath" to GodotFileMode.USER_CREATE_ONCE
            "scene.tscn" -> "scenePath" to GodotFileMode.SCENE_CREATE_ONCE
            "data.tres" -> "dataPath" to GodotFileMode.RESOURCE_REBUILDABLE
            "module.json" -> "moduleJsonPath" to GodotFileMode.STATIC_REBUILDABLE
            "readme.md" -> "readmePath" to GodotFileMode.STATIC_REBUILDABLE
            else -> error("Unknown template id: $templateId")
        }
    }
}

/** How a generated file is treated for create/overwrite. Drives idempotency rules. */
enum class GodotFileMode {
    /** `<Pascal>Generated.gd` under generated/. Rebuilt by `--force`; blocked without it. */
    GENERATED_REBUILDABLE,

    /** User stub under src/. Created once; NEVER overwritten (even with `--force`). */
    USER_CREATE_ONCE,

    /** Scene under scenes/. Created once by default (rebuild via --force). */
    SCENE_CREATE_ONCE,

    /** `.tres` data resource under generated/resources/. Rebuilt by `--force`. */
    RESOURCE_REBUILDABLE,

    /** Static artifact (module.json / README). Rebuilt by `--force`. */
    STATIC_REBUILDABLE,
}
