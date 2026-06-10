package com.dqc.egsengine.feature.scaffold

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.ClientAppNavigationWiring
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import java.io.File

internal object ScaffoldTestFixtures {
    private val templateEngine = TemplateEngine(TemplateRegistry())

    fun androidModuleGenerator(): ModuleGenerator = ModuleGenerator(AndroidModuleGenerator(SettingsGradleUpdater(), templateEngine))

    fun previewModule(
        projectRoot: File,
        template: ModuleTemplate,
    ): List<Pair<String, String?>> = when (template.projectType.uppercase()) {
        "KMP", "KMP_ANDROID" -> {
            val config =
                SubProjectConfig(
                    platform = Platform.KMP,
                    path = ".",
                    basePackage =
                    template.basePackage
                        ?: template.packageName.substringBefore(".feature.", template.packageName),
                    conventionPluginId = template.conventionPluginId,
                )
            KmpModuleGenerator(SettingsGradleUpdater(), templateEngine)
                .preview(projectRoot, template.name, config)
                .map { it.path to it.content }
        }

        else ->
            androidModuleGenerator()
                .preview(projectRoot, template)
                .map { it.path to it.content }
    }

    fun pageScaffolder(): PageScaffolder = PageScaffolder(
        configReader = EgsConfigReader(),
        useCaseScanner = UseCaseScanner(),
        diUpdater = FeatureDiUpdater(),
        templateEngine = templateEngine,
        clientAppNavigationWiring = ClientAppNavigationWiring(),
    )
}
