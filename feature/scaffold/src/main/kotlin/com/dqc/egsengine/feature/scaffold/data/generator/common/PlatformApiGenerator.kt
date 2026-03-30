package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import java.io.File

interface PlatformApiGenerator {
    val platform: Platform

    fun generate(
        projectRoot: File,
        moduleName: String,
        spec: SwaggerSpec,
        config: SubProjectConfig,
    ): List<GeneratedFile>
}
