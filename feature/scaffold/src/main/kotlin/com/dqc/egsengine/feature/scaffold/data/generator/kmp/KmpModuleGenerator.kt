/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * KMP / Compose Multiplatform feature module scaffold (commonMain), aligned with egs-kmp-template.
 */
class KmpModuleGenerator(
    private val settingsUpdater: SettingsGradleUpdater,
    private val templateEngine: TemplateEngine,
) : PlatformModuleGenerator {
    private val logger = LoggerFactory.getLogger(KmpModuleGenerator::class.java)

    override val platform: Platform = Platform.KMP

    override fun preview(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        val model = config.toKmpModuleTemplateModel(moduleName)
        val gen = KmpModuleTemplateRenderer(templateEngine, model)
        val moduleDir = "feature/$moduleName"
        return listOf(
            GeneratedFile("$moduleDir/build.gradle.kts", gen.renderBuildGradle(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathRootKoinModule()}", gen.renderRootKoinModule(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathDataModule()}", gen.renderDataModule(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathDomainModule()}", gen.renderDomainModule(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathPresentationModule()}", gen.renderPresentationModule(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathRepository()}", gen.renderRepository(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathRepositoryImpl()}", gen.renderRepositoryImpl(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathContract()}", gen.renderContract(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathViewModel()}", gen.renderViewModel(projectRoot)),
            GeneratedFile("$moduleDir/${gen.pathScreen()}", gen.renderScreen(projectRoot)),
        )
    }

    override fun generate(
        projectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ): List<File> {
        val created = mutableListOf<File>()
        for (entry in preview(projectRoot, moduleName, config)) {
            val file = projectRoot.resolve(entry.path)
            file.parentFile.mkdirs()
            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }
            created.add(file)
            logger.debug("Created: {}", entry.path)
        }
        logger.info("Generated {} files for KMP module '{}'", created.size, moduleName)
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
}
