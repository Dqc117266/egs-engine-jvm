package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.PageKotlinTemplateRenderer
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.effectiveBasePackage
import com.dqc.egsengine.feature.scaffold.domain.resolveScaffoldBaseClasses
import com.dqc.egsengine.template.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Page scaffold: Contract, ViewModel, Screen (Compose) via FreeMarker templates.
 */
class PageScaffolder(
    private val configReader: EgsConfigReader,
    private val useCaseScanner: UseCaseScanner,
    private val diUpdater: FeatureDiUpdater,
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(PageScaffolder::class.java)

    fun scaffold(
        projectRoot: File,
        moduleName: String,
        pageName: String,
        useCases: List<UseCaseInfo>,
        dryRun: Boolean = false,
    ): PageScaffoldResult {
        logger.info("Scaffolding page '$pageName' in module '$moduleName'")

        val config = configReader.read(projectRoot)
        val basePackage = config.effectiveBasePackage()

        val modulePackage = if (basePackage != null) {
            "$basePackage.feature.$moduleName"
        } else {
            "com.example.feature.$moduleName"
        }

        val baseClasses = config.resolveScaffoldBaseClasses(includeRetrofitProvider = true)

        val template = PageTemplate(
            pageName = pageName.replaceFirstChar { it.uppercase() },
            moduleName = moduleName,
            modulePackage = modulePackage,
            useCases = useCases,
            basePackage = basePackage,
            baseClassPackages = baseClasses,
        )

        val previewFiles = previewFiles(template, projectRoot)

        if (dryRun) {
            return PageScaffoldResult(
                pageName = template.pageName,
                moduleName = moduleName,
                files = previewFiles,
                dryRun = true,
            )
        }

        val camelName = template.pageName.replaceFirstChar { it.lowercase() }
        val screenDir = projectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/${modulePackage.replace(".", "/")}/presentation/screen/$camelName",
        )
        require(!screenDir.exists()) {
            "Page directory already exists: ${screenDir.relativeTo(projectRoot).path}"
        }

        val renderer = PageKotlinTemplateRenderer(templateEngine, template)

        val contractFile = projectRoot.resolve("feature/$moduleName/${renderer.pathContract()}")
        contractFile.parentFile.mkdirs()
        contractFile.writeText(renderer.renderContract(projectRoot))

        val vmFile = projectRoot.resolve("feature/$moduleName/${renderer.pathViewModel()}")
        vmFile.parentFile.mkdirs()
        vmFile.writeText(renderer.renderViewModel(projectRoot))

        val screenFile = projectRoot.resolve("feature/$moduleName/${renderer.pathScreen()}")
        screenFile.parentFile.mkdirs()
        screenFile.writeText(renderer.renderScreen(projectRoot))

        diUpdater.updatePresentationModule(
            projectRoot = projectRoot,
            moduleName = moduleName,
            modulePackage = modulePackage,
            pageName = template.pageName,
            useCases = useCases,
        )

        logger.info("Successfully scaffolded page '${template.pageName}' in module '$moduleName'")

        return PageScaffoldResult(
            pageName = template.pageName,
            moduleName = moduleName,
            files = previewFiles,
            dryRun = false,
        )
    }

    private fun previewFiles(template: PageTemplate, projectRoot: File): List<GeneratedFileInfo> {
        val renderer = PageKotlinTemplateRenderer(templateEngine, template)
        val modulePath = "feature/${template.moduleName}"
        return listOf(
            GeneratedFileInfo("$modulePath/${renderer.pathContract()}", renderer.renderContract(projectRoot)),
            GeneratedFileInfo("$modulePath/${renderer.pathViewModel()}", renderer.renderViewModel(projectRoot)),
            GeneratedFileInfo("$modulePath/${renderer.pathScreen()}", renderer.renderScreen(projectRoot)),
        )
    }
}
