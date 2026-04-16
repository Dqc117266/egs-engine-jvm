package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import com.dqc.egsengine.feature.init.domain.model.toEgsConfig
import kotlinx.serialization.json.Json
import java.io.File

class EgsConfigReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(projectRoot: File): EgsConfig {
        val configFile = projectRoot.resolve(".egs/config.json")
        require(configFile.exists()) {
            "No .egs/config.json found at ${projectRoot.absolutePath}. Run 'egs init' first."
        }
        return json.decodeFromString(EgsConfig.serializer(), configFile.readText())
    }

    /**
     * Reads config for scaffolding under [scaffoldRoot] (usually `client/` in a workspace).
     * Tries `scaffoldRoot/.egs/config.json`, then [workspaceRoot]`.egs/workspace.json` (client project).
     */
    fun readForScaffold(scaffoldRoot: File, workspaceRoot: File = scaffoldRoot): EgsConfig {
        val direct = scaffoldRoot.resolve(".egs/config.json")
        if (direct.exists()) {
            return json.decodeFromString(EgsConfig.serializer(), direct.readText())
        }
        val workspaceFile = workspaceRoot.resolve(".egs/workspace.json")
        if (workspaceFile.exists()) {
            val workspace = json.decodeFromString(WorkspaceConfig.serializer(), workspaceFile.readText())
            val client = workspace.projects["client"]
                ?: throw IllegalArgumentException(
                    "No 'client' entry in .egs/workspace.json. Available: ${workspace.projects.keys}",
                )
            return client.toEgsConfig(workspace.name)
        }
        throw IllegalArgumentException(
            "No .egs/config.json under ${scaffoldRoot.absolutePath} and no .egs/workspace.json under ${workspaceRoot.absolutePath}. " +
                "Run 'egs init' in the client project or use a workspace with .egs/workspace.json.",
        )
    }
}
