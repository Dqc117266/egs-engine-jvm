package com.dqc.egsengine.feature.scaffold.domain.model

data class SpringBootTemplate(
    val moduleName: String,
    val basePackage: String,
    val conventionPluginId: String? = null,
    val coreModulePath: String = ":core",
    val sharedModulePath: String = ":shared",
)
