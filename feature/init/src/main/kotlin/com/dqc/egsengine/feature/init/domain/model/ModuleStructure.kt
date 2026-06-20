package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModuleStructure(
    val layers: List<String> = emptyList(),
    val hasRes: Boolean = false,
    /** Godot module layout: the per-module subdirectories (api/generated/src/scenes/resources/tests). */
    val moduleDirs: List<String> = emptyList(),
)
