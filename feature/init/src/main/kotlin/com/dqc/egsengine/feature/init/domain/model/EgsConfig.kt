package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScaffoldOverrides(
    val baseViewModelFqn: String? = null,
    val baseFragmentFqn: String? = null,
    val basePackage: String? = null,
    /**
     * Where generated ViewModels live under `presentation/` for Koin imports: `"screen"` (default for `create screen`
     * output) or `"fragment"` (legacy). When null, `create screen` detects from existing `presentation/screen` vs
     * `presentation/fragment` directories.
     */
    val androidPresentationLayout: String? = null,
)

@Serializable
data class EgsConfig(
    val projectName: String,
    val projectType: String,
    val rootPath: String,
    val conventionPluginId: String? = null,
    val basePackage: String? = null,
    val moduleStructure: ModuleStructure,
    val baseClasses: List<BaseClassInfo>,
    val scaffoldOverrides: ScaffoldOverrides? = null,
)
