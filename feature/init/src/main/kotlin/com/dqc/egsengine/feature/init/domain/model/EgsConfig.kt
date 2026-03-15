package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EgsConfig(
    val projectName: String,
    val projectType: String,
    val rootPath: String,
    val conventionPluginId: String? = null,
    val basePackage: String? = null,
    val moduleStructure: ModuleStructure,
    val baseClasses: List<BaseClassInfo>,
)
