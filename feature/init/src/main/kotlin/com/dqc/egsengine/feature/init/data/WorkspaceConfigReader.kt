package com.dqc.egsengine.feature.init.data

import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

class WorkspaceConfigReader {
    private val logger = LoggerFactory.getLogger(WorkspaceConfigReader::class.java)

    private val json = Json { ignoreUnknownKeys = true }

    fun read(projectRoot: File): WorkspaceConfig {
        val workspaceFile = projectRoot.resolve(".egs/workspace.json")
        if (workspaceFile.exists()) {
            logger.debug("Reading workspace config from: {}", workspaceFile.absolutePath)
            return json.decodeFromString(WorkspaceConfig.serializer(), workspaceFile.readText())
        }

        val legacyFile = projectRoot.resolve(".egs/config.json")
        if (legacyFile.exists()) {
            logger.info("No workspace.json found; wrapping legacy config.json")
            val legacy = json.decodeFromString(EgsConfig.serializer(), legacyFile.readText())
            return wrapLegacy(legacy)
        }

        throw IllegalStateException(
            "No .egs/workspace.json or .egs/config.json found at ${projectRoot.absolutePath}. Run 'egs init' first.",
        )
    }

    fun hasWorkspaceConfig(projectRoot: File): Boolean =
        projectRoot.resolve(".egs/workspace.json").exists()

    fun hasLegacyConfig(projectRoot: File): Boolean =
        projectRoot.resolve(".egs/config.json").exists()

    private fun wrapLegacy(legacy: EgsConfig): WorkspaceConfig {
        val platform = when (legacy.projectType.uppercase()) {
            "ANDROID" -> Platform.ANDROID
            "KMP" -> Platform.KMP
            "KMP_ANDROID" -> Platform.KMP_ANDROID
            "KOTLIN_JVM" -> Platform.KOTLIN_JVM
            else -> Platform.KOTLIN_JVM
        }

        val subProject = SubProjectConfig(
            platform = platform,
            path = ".",
            basePackage = legacy.basePackage ?: "com.example",
            conventionPluginId = legacy.conventionPluginId,
            moduleStructure = legacy.moduleStructure,
            baseClasses = legacy.baseClasses,
            scaffoldOverrides = legacy.scaffoldOverrides,
        )

        return WorkspaceConfig(
            name = legacy.projectName,
            projects = mapOf("client" to subProject),
        )
    }
}
