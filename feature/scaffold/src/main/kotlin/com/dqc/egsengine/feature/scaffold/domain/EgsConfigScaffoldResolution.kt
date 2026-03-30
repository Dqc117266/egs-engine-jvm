package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

// ---------------------------------------------------------------------------
// Legacy EgsConfig extensions (backward compat for single-project mode)
// ---------------------------------------------------------------------------

fun EgsConfig.effectiveBasePackage(): String? =
    scaffoldOverrides?.basePackage ?: basePackage

val EgsConfig.isAndroid: Boolean
    get() = projectType in setOf("ANDROID", "KMP_ANDROID")

private fun EgsConfig.resolveNamedBaseClass(className: String): String? {
    val fromOverride = when (className) {
        "BaseViewModel" -> scaffoldOverrides?.baseViewModelFqn
        "BaseFragment" -> scaffoldOverrides?.baseFragmentFqn
        else -> null
    }
    if (!fromOverride.isNullOrBlank()) return fromOverride.trim()
    return baseClasses.find { it.name == className }?.let { "${it.packageName}.$className" }
}

fun EgsConfig.resolveScaffoldBaseClasses(includeRetrofitProvider: Boolean): BaseClassPackages {
    val bp = effectiveBasePackage()
    return BaseClassPackages(
        baseViewModel = resolveNamedBaseClass("BaseViewModel"),
        baseFragment = resolveNamedBaseClass("BaseFragment"),
        resultClass = bp?.let { "$it.feature.base.domain.result.Result" },
        retrofitProvider = if (includeRetrofitProvider) {
            bp?.let { "$it.feature.common.network.DynamicRetrofitProvider" }
        } else {
            null
        },
    )
}

// ---------------------------------------------------------------------------
// SubProjectConfig extensions (new workspace-aware multi-platform)
// ---------------------------------------------------------------------------

fun SubProjectConfig.effectiveBasePackage(): String =
    scaffoldOverrides?.basePackage ?: basePackage

val SubProjectConfig.isAndroid: Boolean
    get() = platform in setOf(Platform.ANDROID, Platform.KMP_ANDROID)

private fun SubProjectConfig.resolveNamedBaseClass(className: String): String? {
    val fromOverride = when (className) {
        "BaseViewModel" -> scaffoldOverrides?.baseViewModelFqn
        "BaseFragment" -> scaffoldOverrides?.baseFragmentFqn
        else -> null
    }
    if (!fromOverride.isNullOrBlank()) return fromOverride.trim()
    return baseClasses.find { it.name == className }?.let { "${it.packageName}.$className" }
}

fun SubProjectConfig.resolveScaffoldBaseClasses(includeRetrofitProvider: Boolean = false): BaseClassPackages {
    val bp = effectiveBasePackage()
    return when (platform) {
        Platform.ANDROID, Platform.KMP, Platform.KMP_ANDROID -> BaseClassPackages(
            baseViewModel = resolveNamedBaseClass("BaseViewModel"),
            baseFragment = resolveNamedBaseClass("BaseFragment"),
            resultClass = "$bp.feature.base.domain.result.Result",
            retrofitProvider = if (includeRetrofitProvider) {
                "$bp.feature.common.network.DynamicRetrofitProvider"
            } else {
                null
            },
        )
        Platform.SPRING_BOOT, Platform.KOTLIN_JVM -> BaseClassPackages()
        Platform.VUE3 -> BaseClassPackages()
    }
}

fun SubProjectConfig.toModuleTemplate(moduleName: String): ModuleTemplate {
    val normalizedModule = moduleName.replace("-", "").replace("_", "")
    val bp = effectiveBasePackage()
    val featurePackage = "$bp.feature.$normalizedModule"
    val isAndroid = this.isAndroid
    val namespace = if (isAndroid) featurePackage else null

    return ModuleTemplate(
        name = moduleName,
        packageName = featurePackage,
        conventionPluginId = conventionPluginId,
        layers = moduleStructure?.layers ?: listOf("data", "domain", "presentation"),
        hasRes = moduleStructure?.hasRes ?: false,
        namespace = namespace,
        projectType = platform.name,
        platform = platform,
        basePackage = bp,
        baseClassPackages = resolveScaffoldBaseClasses(includeRetrofitProvider = false),
        apiResultClass = if (isAndroid) "$bp.feature.base.data.retrofit.ApiResult" else null,
        commonResultClass = if (isAndroid) "$bp.feature.base.data.retrofit.CommonResult" else null,
        toResultPackage = if (isAndroid) "$bp.feature.base.data.retrofit" else null,
    )
}
