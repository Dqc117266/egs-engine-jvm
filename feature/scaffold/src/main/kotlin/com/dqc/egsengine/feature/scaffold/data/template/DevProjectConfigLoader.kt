package com.dqc.egsengine.feature.scaffold.data.template

import com.dqc.egsengine.feature.scaffold.data.TemplateRenameRecipe
import com.dqc.egsengine.feature.scaffold.data.TemplateRenameRecipes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class WorkspaceProjectEntry(
    val platform: String? = null,
    val path: String,
    val basePackage: String? = null,
    val templateUrl: String? = null,
)

@Serializable
data class WorkspaceConfigFile(
    val name: String? = null,
    val projects: Map<String, WorkspaceProjectEntry> = emptyMap(),
)

@Serializable
private data class EgsConfigFile(
    val projectName: String? = null,
    val basePackage: String? = null,
)

data class ProjectSyncContext(
    val projectDir: File,
    val projectName: String,
    val basePackage: String?,
    val templateDir: File?,
    val recipe: TemplateRenameRecipe?,
)

object DevProjectConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadSyncContext(projectDir: File): ProjectSyncContext {
        val normalized = projectDir.absoluteFile
        val workspaceRoot = findWorkspaceRoot(normalized)
        val configFile = normalized.resolve(".egs/config.json")
        val config =
            if (configFile.isFile) {
                json.decodeFromString<EgsConfigFile>(configFile.readText())
            } else {
                EgsConfigFile()
            }

        val workspaceEntry = workspaceRoot?.let { findWorkspaceEntry(it, normalized) }
        val templateUrl = workspaceEntry?.templateUrl
        val templateDir = templateUrl?.let { File(it).takeIf { f -> f.isDirectory } }
        val recipe = templateDir?.let { TemplateRenameRecipes.detectFromPath(it.absolutePath) }

        return ProjectSyncContext(
            projectDir = normalized,
            projectName =
            config.projectName
                ?: workspaceRoot?.let { json.decodeFromString<WorkspaceConfigFile>(it.resolve(".egs/workspace.json").readText()).name }
                ?: normalized.name,
            basePackage = config.basePackage ?: workspaceEntry?.basePackage,
            templateDir = templateDir,
            recipe = recipe,
        )
    }

    private fun findWorkspaceRoot(start: File): File? {
        var current: File? = start
        while (current != null) {
            if (current.resolve(".egs/workspace.json").isFile) return current
            current = current.parentFile
        }
        return null
    }

    private fun findWorkspaceEntry(
        workspaceRoot: File,
        projectDir: File,
    ): WorkspaceProjectEntry? {
        val workspaceFile = workspaceRoot.resolve(".egs/workspace.json")
        if (!workspaceFile.isFile) return null
        val workspace = json.decodeFromString<WorkspaceConfigFile>(workspaceFile.readText())
        return workspace.projects.values.firstOrNull { entry ->
            workspaceRoot.resolve(entry.path).absoluteFile == projectDir
        }
    }
}
