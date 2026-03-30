package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import java.io.File

interface PlatformModuleGenerator {
    val platform: Platform

    fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile>

    fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File>

    fun updateSettings(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    )
}
