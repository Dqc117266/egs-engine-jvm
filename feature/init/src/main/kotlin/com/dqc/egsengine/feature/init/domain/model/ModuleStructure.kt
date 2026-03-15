package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ModuleStructure(
    val layers: List<String>,
    val hasRes: Boolean,
)
