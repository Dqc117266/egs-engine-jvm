package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages

/**
 * Effective base package for scaffolding: optional [EgsConfig.scaffoldOverrides.basePackage] wins over auto-detected [EgsConfig.basePackage].
 */
fun EgsConfig.effectiveBasePackage(): String? =
    scaffoldOverrides?.basePackage ?: basePackage

private fun EgsConfig.resolveNamedBaseClass(className: String): String? {
    val fromOverride = when (className) {
        "BaseViewModel" -> scaffoldOverrides?.baseViewModelFqn
        "BaseFragment" -> scaffoldOverrides?.baseFragmentFqn
        else -> null
    }
    if (!fromOverride.isNullOrBlank()) return fromOverride.trim()
    return baseClasses.find { it.name == className }?.let { "${it.packageName}.$className" }
}

/**
 * @param includeRetrofitProvider when true, fills [BaseClassPackages.retrofitProvider] from [effectiveBasePackage] (Swagger/page flows).
 */
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
