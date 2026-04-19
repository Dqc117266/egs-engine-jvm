package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceConfig(
    val name: String,
    val version: String = "1",
    val projects: Map<String, SubProjectConfig>,
    val swagger: SwaggerSyncConfig? = null,
)

@Serializable
data class SubProjectConfig(
    val platform: Platform,
    val path: String,
    val basePackage: String,
    val templateUrl: String? = null,
    val conventionPluginId: String? = null,
    val moduleStructure: ModuleStructure? = null,
    val baseClasses: List<BaseClassInfo> = emptyList(),
    val scaffoldOverrides: ScaffoldOverrides? = null,
)

@Serializable
data class SwaggerSyncConfig(
    val backendProject: String = "backend",
    val clientProject: String = "client",
    val baseUrl: String = "http://localhost:8080",
    val docPath: String = "/v3/api-docs",
    /**
     * Per client feature module: override Swagger URL or path for that module only.
     * Key = client module name (e.g. `todolist`). Priority: [SwaggerModuleConfig.url] >
     * [SwaggerModuleConfig.docPath] > global [docPath].
     */
    val modules: Map<String, SwaggerModuleConfig> = emptyMap(),
)

@Serializable
data class SwaggerModuleConfig(
    /** Optional label for documentation; not used by the resolver. */
    val backendModule: String? = null,
    /** Path appended to [SwaggerSyncConfig.baseUrl], e.g. `/v3/api-docs/todo`. */
    val docPath: String? = null,
    /** Full URL override; takes precedence over [docPath]. */
    val url: String? = null,
)

/**
 * Maps workspace client config to legacy [EgsConfig] for scaffold commands that still read [EgsConfig].
 */
fun SubProjectConfig.toEgsConfig(projectName: String): EgsConfig = EgsConfig(
    projectName = projectName,
    projectType = platform.name,
    rootPath = path,
    conventionPluginId = conventionPluginId,
    basePackage = basePackage,
    moduleStructure = moduleStructure ?: ModuleStructure(
        layers = listOf("data", "domain", "presentation"),
        hasRes = false,
    ),
    baseClasses = baseClasses,
    scaffoldOverrides = scaffoldOverrides,
)
