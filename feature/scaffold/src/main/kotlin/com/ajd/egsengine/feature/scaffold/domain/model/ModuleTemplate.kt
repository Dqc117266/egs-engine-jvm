package com.ajd.egsengine.feature.scaffold.domain.model

data class ModuleTemplate(
    val name: String,
    val packageName: String,
    val conventionPluginId: String?,
    val layers: List<String>,
    val hasRes: Boolean,
    val namespace: String?,
    val projectType: String,
)
