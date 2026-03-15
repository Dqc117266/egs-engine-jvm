package com.dqc.egsengine.feature.analyzer.domain.model

data class ModuleInfo(
    val name: String,
    val path: String,
    val type: ProjectType,
    val plugins: List<String>,
    val dependencies: List<String>,
    val hasAndroidManifest: Boolean = false,
    val sourceSetDirs: List<String> = emptyList(),
)
