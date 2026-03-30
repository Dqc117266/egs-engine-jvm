package com.dqc.egsengine.feature.scaffold.data.config

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Scaffold-layer helper that reads workspace config and resolves the correct
 * [SubProjectConfig] for a given target platform or project key.
 */
class WorkspaceConfigResolver(
    private val workspaceConfigReader: WorkspaceConfigReader,
) {
    private val logger = LoggerFactory.getLogger(WorkspaceConfigResolver::class.java)

    fun readWorkspace(projectRoot: File): WorkspaceConfig =
        workspaceConfigReader.read(projectRoot)

    fun isWorkspaceProject(projectRoot: File): Boolean =
        workspaceConfigReader.hasWorkspaceConfig(projectRoot)

    fun resolveByKey(projectRoot: File, projectKey: String): SubProjectConfig {
        val workspace = readWorkspace(projectRoot)
        return workspace.projects[projectKey]
            ?: throw IllegalArgumentException(
                "No project '$projectKey' found in workspace.json. Available: ${workspace.projects.keys}",
            )
    }

    fun resolveByPlatform(projectRoot: File, platform: Platform): SubProjectConfig {
        val workspace = readWorkspace(projectRoot)
        return workspace.projects.values.firstOrNull { it.platform == platform }
            ?: throw IllegalArgumentException(
                "No project with platform '$platform' found in workspace.json.",
            )
    }

    fun resolveClient(projectRoot: File): SubProjectConfig =
        resolveByKey(projectRoot, "client")

    fun resolveBackend(projectRoot: File): SubProjectConfig =
        resolveByKey(projectRoot, "backend")

    fun resolveAdmin(projectRoot: File): SubProjectConfig =
        resolveByKey(projectRoot, "admin")

    fun resolveSwaggerUrl(projectRoot: File): String {
        val workspace = readWorkspace(projectRoot)
        val swagger = workspace.swagger
            ?: throw IllegalStateException("No swagger sync config in workspace.json")
        return "${swagger.baseUrl}${swagger.docPath}"
    }
}
