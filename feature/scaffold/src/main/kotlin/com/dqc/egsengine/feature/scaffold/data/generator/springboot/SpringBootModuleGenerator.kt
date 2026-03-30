package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Creates empty Spring Boot feature module skeletons:
 * - build.gradle.kts with convention plugin + Spring dependencies
 * - Empty layer packages (data/domain/presentation)
 * - Registered in settings.gradle.kts
 */
class SpringBootModuleGenerator(
    private val settingsUpdater: SettingsGradleUpdater,
) : PlatformModuleGenerator {

    private val logger = LoggerFactory.getLogger(SpringBootModuleGenerator::class.java)

    override val platform: Platform = Platform.SPRING_BOOT

    override fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val files = mutableListOf<GeneratedFile>()
        val moduleDir = "feature/$moduleName"
        val basePackage = config.basePackage
        val pkgPath = "$basePackage.feature.$moduleName".replace('.', '/')

        files.add(GeneratedFile("$moduleDir/build.gradle.kts", generateBuildFile(config)))

        val layers = listOf("data/entity", "data/repository", "domain/model", "domain/repository", "domain/service", "presentation/controller", "presentation/dto")
        for (layer in layers) {
            files.add(GeneratedFile("$moduleDir/src/main/kotlin/$pkgPath/$layer/.gitkeep", ""))
        }

        return files
    }

    override fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File> {
        val subProjectRoot = projectRoot.resolve(config.path)
        val created = mutableListOf<File>()

        for (entry in preview(projectRoot, moduleName, config)) {
            val file = subProjectRoot.resolve(entry.path)
            file.parentFile.mkdirs()
            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }
            created.add(file)
            logger.debug("Created: {}", entry.path)
        }

        logger.info("Generated {} files for Spring Boot module '{}'", created.size, moduleName)
        return created
    }

    override fun updateSettings(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        val subProjectRoot = projectRoot.resolve(config.path)
        settingsUpdater.update(subProjectRoot, moduleName)
    }

    private fun generateBuildFile(config: SubProjectConfig): String = buildString {
        appendLine("plugins {")
        if (config.conventionPluginId != null) {
            appendLine("    id(\"${config.conventionPluginId}\")")
        } else {
            appendLine("    id(\"org.jetbrains.kotlin.jvm\")")
        }
        appendLine("}")
        appendLine()
        appendLine("dependencies {")
        appendLine("    implementation(project(\":core\"))")
        appendLine("    implementation(project(\":shared\"))")
        appendLine("}")
    }
}
