package com.dqc.egsengine.feature.scaffold.data.generator.godot

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Reads a Godot project's `.egs/generator.json` into a [GodotGeneratorContract].
 *
 * Unlike [com.dqc.egsengine.feature.init.data.WorkspaceConfigReader] (which is scoped to
 * `workspace.json`/`config.json`), this reader is dedicated to the generator contract —
 * the single source of truth for entity generation paths, templates, and registry.
 */
class GodotGeneratorContractReader {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(projectRoot: File): GodotGeneratorContract {
        val contractFile = projectRoot.resolve(".egs/generator.json")
        require(contractFile.exists()) {
            "Not an egs-godot project root: ${projectRoot.absolutePath}. " +
                "Expected .egs/generator.json (create the project with `egs new game --engine godot`)."
        }
        return json.decodeFromString(GodotGeneratorContract.serializer(), contractFile.readText())
    }

    /** Read and require the named command; error lists valid commands if absent. */
    fun requireCommand(
        projectRoot: File,
        command: String,
    ): Pair<GodotGeneratorContract, GodotCommandSpec> {
        val contract = read(projectRoot)
        val spec =
            contract.commands[command]
                ?: error(
                    "Unknown entity type '$command'. Valid: ${contract.commands.keys.sorted()}.",
                )
        return contract to spec
    }
}
