package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import java.io.File

class AndroidApiGenerator(
    private val swaggerCodeGenerator: SwaggerCodeGenerator,
) : PlatformApiGenerator {

    override val platform: Platform = Platform.ANDROID

    override fun generate(
        projectRoot: File,
        moduleName: String,
        spec: SwaggerSpec,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val template = AndroidModuleGenerator.toModuleTemplate(moduleName, config).let { base ->
            base.copy(
                baseClassPackages = base.baseClassPackages.copy(
                    retrofitProvider = config.basePackage.let { "$it.feature.common.network.DynamicRetrofitProvider" },
                ),
                apiResultClass = "${config.basePackage}.feature.base.data.retrofit.ApiResult",
                commonResultClass = "${config.basePackage}.feature.base.data.retrofit.CommonResult",
                toResultPackage = "${config.basePackage}.feature.base.data.retrofit",
            )
        }
        return swaggerCodeGenerator.generateToCommon(template, spec)
    }
}
