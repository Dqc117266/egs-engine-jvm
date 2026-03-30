package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Generates Spring Boot controller/service stubs from Swagger spec (reverse direction).
 *
 * Skeleton -- concrete generation to be implemented later.
 */
class SpringBootApiGenerator : PlatformApiGenerator {

    private val logger = LoggerFactory.getLogger(SpringBootApiGenerator::class.java)

    override val platform: Platform = Platform.SPRING_BOOT

    override fun generate(
        projectRoot: File,
        moduleName: String,
        spec: SwaggerSpec,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        logger.info("SpringBootApiGenerator.generate() called for module '{}' -- not yet implemented", moduleName)
        TODO("Spring Boot API generation from Swagger not yet implemented")
    }
}
