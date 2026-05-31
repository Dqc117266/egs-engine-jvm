/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.feature.scaffold.domain.toModuleTemplate
import java.io.File

/**
 * KMP client API sync: generates `generate/` tree (Ktorfit + domain) under `commonMain`.
 */
class KmpApiGenerator(
    private val kmpSwaggerCodeGenerator: KmpSwaggerCodeGenerator,
) : PlatformApiGenerator {

    override val platform: Platform = Platform.KMP

    override fun generate(
        projectRoot: File,
        moduleName: String,
        spec: SwaggerSpec,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val template = config.toKmpSwaggerModuleTemplate(moduleName)
        return kmpSwaggerCodeGenerator.generateToCommon(template, spec, projectRoot)
    }
}

internal fun SubProjectConfig.toKmpSwaggerModuleTemplate(moduleName: String): ModuleTemplate {
    val base = toModuleTemplate(moduleName)
    val coreBase = "$basePackage.core.base"
    return base.copy(
        baseClassPackages = BaseClassPackages(
            baseViewModel = base.baseClassPackages.baseViewModel,
            baseFragment = base.baseClassPackages.baseFragment,
            resultClass = "$coreBase.network.domain.Result",
            pageResultClass = "$coreBase.ui.PageResult",
            retrofitProvider = base.baseClassPackages.retrofitProvider,
        ),
        apiResultClass = "$coreBase.network.NetworkResult",
        commonResultClass = "$coreBase.network.data.CommonResult",
        toResultPackage = "$coreBase.network.data",
    )
}
