package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.PageKotlinTemplateRenderer
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPageTemplateRenderer
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.init.domain.model.EgsConfig
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
        /** Workspace root when [projectRoot] is `client/` (for `.egs/workspace.json`). */
        workspaceRoot: File = projectRoot,
    ): PageScaffoldResult {
        logger.info("Scaffolding page '$pageName' in module '$moduleName'")

        val config = configReader.readForScaffold(projectRoot, workspaceRoot)
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

        val moduleDir = projectRoot.resolve("feature/$moduleName")
        val kotlinRootRel = kotlinSourceRootRelative(moduleDir)
        val useKmpPageTemplates = useKmpPageTemplates(config, kotlinRootRel)
        val previewFiles = previewFiles(template, projectRoot, kotlinRootRel, useKmpPageTemplates)

        if (dryRun) {
            return PageScaffoldResult(
                pageName = template.pageName,
                moduleName = moduleName,
                files = previewFiles,
                dryRun = true,
            )
        }

        val camelName = template.pageName.replaceFirstChar { it.lowercase() }
        val presentationSegment =
            if (useKmpPageTemplates) "presentation/$camelName" else "presentation/screen/$camelName"
        val screenDir = projectRoot.resolve(
            "feature/$moduleName/$kotlinRootRel/${modulePackage.replace(".", "/")}/$presentationSegment",
        )
        require(!screenDir.exists()) {
            "Page directory already exists: ${screenDir.relativeTo(projectRoot).path}"
        }

        if (useKmpPageTemplates) {
            val kmpRenderer = KmpPageTemplateRenderer(templateEngine, template, kotlinRootRel)
            val contractFile = projectRoot.resolve("feature/$moduleName/${kmpRenderer.pathContract()}")
            contractFile.parentFile.mkdirs()
            contractFile.writeText(kmpRenderer.renderContract(projectRoot))

            val vmFile = projectRoot.resolve("feature/$moduleName/${kmpRenderer.pathViewModel()}")
            vmFile.parentFile.mkdirs()
            vmFile.writeText(kmpRenderer.renderViewModel(projectRoot))

            val screenFile = projectRoot.resolve("feature/$moduleName/${kmpRenderer.pathScreen()}")
            screenFile.parentFile.mkdirs()
            screenFile.writeText(kmpRenderer.renderScreen(projectRoot))
        } else {
            val renderer = PageKotlinTemplateRenderer(templateEngine, template, kotlinRootRel)
            val contractFile = projectRoot.resolve("feature/$moduleName/${renderer.pathContract()}")
            contractFile.parentFile.mkdirs()
            contractFile.writeText(renderer.renderContract(projectRoot))

            val vmFile = projectRoot.resolve("feature/$moduleName/${renderer.pathViewModel()}")
            vmFile.parentFile.mkdirs()
            vmFile.writeText(renderer.renderViewModel(projectRoot))

            val screenFile = projectRoot.resolve("feature/$moduleName/${renderer.pathScreen()}")
            screenFile.parentFile.mkdirs()
            screenFile.writeText(renderer.renderScreen(projectRoot))
        }

        diUpdater.updatePresentationModule(
            projectRoot = projectRoot,
            moduleName = moduleName,
            modulePackage = modulePackage,
            pageName = template.pageName,
            useCases = useCases,
            kotlinRootRel = kotlinRootRel,
            useKmpPresentationLayout = useKmpPageTemplates,
        )

        logger.info("Successfully scaffolded page '${template.pageName}' in module '$moduleName'")

        return PageScaffoldResult(
            pageName = template.pageName,
            moduleName = moduleName,
            files = previewFiles,
            dryRun = false,
        )
    }

    private fun kotlinSourceRootRelative(moduleDir: File): String =
        when {
            moduleDir.resolve("src/commonMain/kotlin").isDirectory -> "src/commonMain/kotlin"
            else -> "src/main/kotlin"
        }

    private fun useKmpPageTemplates(config: EgsConfig, kotlinRootRel: String): Boolean {
        val t = config.projectType.uppercase()
        if (t in setOf("KMP", "KMP_ANDROID")) return true
        return kotlinRootRel == "src/commonMain/kotlin"
    }

    private fun previewFiles(
        template: PageTemplate,
        projectRoot: File,
        kotlinRootRel: String,
        useKmpPageTemplates: Boolean,
    ): List<GeneratedFileInfo> {
        val modulePath = "feature/${template.moduleName}"
        return if (useKmpPageTemplates) {
            val kmpRenderer = KmpPageTemplateRenderer(templateEngine, template, kotlinRootRel)
            listOf(
                GeneratedFileInfo("$modulePath/${kmpRenderer.pathContract()}", kmpRenderer.renderContract(projectRoot)),
                GeneratedFileInfo("$modulePath/${kmpRenderer.pathViewModel()}", kmpRenderer.renderViewModel(projectRoot)),
                GeneratedFileInfo("$modulePath/${kmpRenderer.pathScreen()}", kmpRenderer.renderScreen(projectRoot)),
            )
        } else {
            val renderer = PageKotlinTemplateRenderer(templateEngine, template, kotlinRootRel)
            listOf(
                GeneratedFileInfo("$modulePath/${renderer.pathContract()}", renderer.renderContract(projectRoot)),
                GeneratedFileInfo("$modulePath/${renderer.pathViewModel()}", renderer.renderViewModel(projectRoot)),
                GeneratedFileInfo("$modulePath/${renderer.pathScreen()}", renderer.renderScreen(projectRoot)),
            )
        }
    }
}
