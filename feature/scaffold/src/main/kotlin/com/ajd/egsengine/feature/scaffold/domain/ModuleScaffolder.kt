package com.ajd.egsengine.feature.scaffold.domain

import com.ajd.egsengine.feature.init.domain.model.EgsConfig
import com.ajd.egsengine.feature.scaffold.data.EgsConfigReader
import com.ajd.egsengine.feature.scaffold.data.ModuleGenerator
import com.ajd.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.ajd.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

class ModuleScaffolder(
    private val configReader: EgsConfigReader,
    private val generator: ModuleGenerator,
    private val settingsUpdater: SettingsGradleUpdater,
) {
    private val logger = LoggerFactory.getLogger(ModuleScaffolder::class.java)

    fun scaffold(
        projectRoot: File,
        moduleName: String,
        customPackage: String? = null,
        dryRun: Boolean = false,
    ): ScaffoldResult {
        val config = configReader.read(projectRoot)
        val template = buildTemplate(config, moduleName, customPackage)
        val preview = generator.preview(projectRoot, template)

        if (dryRun) {
            return ScaffoldResult(
                moduleName = moduleName,
                files = preview.map { it.path },
                dryRun = true,
            )
        }

        val moduleDir = projectRoot.resolve("feature/$moduleName")
        require(!moduleDir.exists()) { "Module directory already exists: feature/$moduleName" }

        generator.generate(projectRoot, template)
        settingsUpdater.update(projectRoot, moduleName)

        logger.info("Scaffolded module '$moduleName' at ${moduleDir.absolutePath}")

        return ScaffoldResult(
            moduleName = moduleName,
            files = preview.map { it.path },
            dryRun = false,
        )
    }

    private fun buildTemplate(config: EgsConfig, moduleName: String, customPackage: String?): ModuleTemplate {
        val basePackage = customPackage ?: config.basePackage
        val featurePackage = if (basePackage != null) {
            "$basePackage.feature.${moduleName.replace("-", "")}"
        } else {
            "com.example.feature.${moduleName.replace("-", "")}"
        }

        val isAndroid = config.projectType in listOf("ANDROID", "KMP_ANDROID")
        val namespace = if (isAndroid && config.basePackage != null) {
            "${config.basePackage}.feature.${moduleName.replace("-", "")}"
        } else {
            null
        }

        return ModuleTemplate(
            name = moduleName,
            packageName = featurePackage,
            conventionPluginId = config.conventionPluginId,
            layers = config.moduleStructure.layers,
            hasRes = config.moduleStructure.hasRes,
            namespace = namespace,
            projectType = config.projectType,
        )
    }

    data class ScaffoldResult(
        val moduleName: String,
        val files: List<String>,
        val dryRun: Boolean,
    )
}
