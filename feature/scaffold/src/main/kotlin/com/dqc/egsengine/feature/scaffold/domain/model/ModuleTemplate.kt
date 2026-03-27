package com.dqc.egsengine.feature.scaffold.domain.model

data class ModuleTemplate(
    val name: String,
    val packageName: String,
    val conventionPluginId: String?,
    val layers: List<String>,
    val hasRes: Boolean,
    val namespace: String?,
    val projectType: String,
    val basePackage: String?,
    val baseClassPackages: BaseClassPackages = BaseClassPackages(),
    val apiResultClass: String? = null,
    val commonResultClass: String? = null,
    val toResultPackage: String? = null,
) {
    val isAndroid: Boolean get() = projectType in setOf("ANDROID", "KMP_ANDROID")
}

data class BaseClassPackages(
    val baseViewModel: String? = null,
    val baseFragment: String? = null,
    val resultClass: String? = null,
    val retrofitProvider: String? = null,
)
