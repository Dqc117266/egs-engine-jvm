package com.dqc.egsengine.feature.scaffold.data.generator.vue3

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Generates TypeScript API client + Pinia stores from Swagger spec for Vue3 admin.
 *
 * Skeleton -- concrete .ts/.vue template generation to be implemented later.
 */
class Vue3ApiGenerator : PlatformApiGenerator {

    private val logger = LoggerFactory.getLogger(Vue3ApiGenerator::class.java)

    override val platform: Platform = Platform.VUE3

    override fun generate(
        projectRoot: File,
        moduleName: String,
        spec: SwaggerSpec,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        logger.info("Vue3ApiGenerator.generate() called for module '{}' -- not yet implemented", moduleName)
        TODO("Vue3 API generation from Swagger not yet implemented")
    }
}
