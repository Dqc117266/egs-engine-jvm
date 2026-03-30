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
)
