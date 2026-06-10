package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.scaffold.data.ClientAppNavigationWiring
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.PageKotlinTemplateRenderer
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPageTemplateRenderer
import com.dqc.egsengine.feature.scaffold.domain.effectiveBasePackage
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
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
    private val clientAppNavigationWiring: ClientAppNavigationWiring,
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
        /** See [com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate.pagingOption]. */
        pagingOption: String = "auto",
        skipNav: Boolean = false,
        skipAppWire: Boolean = false,
        withViewModelTest: Boolean = false,
    ): PageScaffoldResult {
        logger.info("Scaffolding page '$pageName' in module '$moduleName'")

        val resolvedUseCases = useCaseScanner.enrichReturnTypesIfMissing(projectRoot, useCases)

        val config = configReader.readForScaffold(projectRoot, workspaceRoot)
        val basePackage = config.effectiveBasePackage()

        val modulePackage =
            if (basePackage != null) {
                "$basePackage.feature.$moduleName"
            } else {
                "com.example.feature.$moduleName"
            }

        val baseClasses = config.resolveScaffoldBaseClasses(includeRetrofitProvider = true)

        val moduleDir = projectRoot.resolve("feature/$moduleName")
        val kotlinRootRel = kotlinSourceRootRelative(moduleDir, config.projectType)
        val useKmpPageTemplates = useKmpPageTemplates(config, kotlinRootRel)
        // KMP pages use `template.core.base.network.domain.Result` (mapper default); ignore `feature.base` Result from config.
        val pageBaseClasses =
            if (useKmpPageTemplates) baseClasses.copy(resultClass = null) else baseClasses

        val template =
            PageTemplate(
                pageName = pageName.replaceFirstChar { it.uppercase() },
                moduleName = moduleName,
                modulePackage = modulePackage,
                useCases = resolvedUseCases,
                basePackage = basePackage,
                baseClassPackages = pageBaseClasses,
                pagingOption = pagingOption,
            )
        val previewFiles =
            previewFiles(
                template,
                projectRoot,
                kotlinRootRel,
                useKmpPageTemplates,
                withViewModelTest,
            )

        if (dryRun) {
            return PageScaffoldResult(
                pageName = template.pageName,
                moduleName = moduleName,
                files = previewFiles,
                dryRun = true,
            )
        }

        val camelName = template.pageName.replaceFirstChar { it.lowercase() }
        val presentationSegment = "presentation/screen/$camelName"
        val screenDir =
            projectRoot.resolve(
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
            if (withViewModelTest) {
                writeViewModelTest(
                    projectRoot = projectRoot,
                    moduleName = moduleName,
                    modulePackage = modulePackage,
                    kotlinRootRel = kotlinRootRel,
                    pascalName = template.pageName,
                    useKmp = true,
                )
            }
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
            if (withViewModelTest) {
                writeViewModelTest(
                    projectRoot = projectRoot,
                    moduleName = moduleName,
                    modulePackage = modulePackage,
                    kotlinRootRel = kotlinRootRel,
                    pascalName = template.pageName,
                    useKmp = false,
                )
            }
        }

        val useScreenPresentationLayout =
            resolveUseScreenPresentationLayout(
                projectRoot = projectRoot,
                moduleName = moduleName,
                modulePackage = modulePackage,
                kotlinRootRel = kotlinRootRel,
                useKmpPageTemplates = useKmpPageTemplates,
                androidPresentationLayoutOverride = config.scaffoldOverrides?.androidPresentationLayout,
            )

        diUpdater.updatePresentationModule(
            projectRoot = projectRoot,
            moduleName = moduleName,
            modulePackage = modulePackage,
            pageName = template.pageName,
            useCases = resolvedUseCases,
            kotlinRootRel = kotlinRootRel,
            useScreenPresentationLayout = useScreenPresentationLayout,
        )

        clientAppNavigationWiring.wireIfPossible(
            clientRoot = projectRoot,
            moduleName = moduleName,
            modulePackage = modulePackage,
            pageName = template.pageName,
            skipNav = skipNav,
            skipAppWire = skipAppWire,
        )

        logger.info("Successfully scaffolded page '${template.pageName}' in module '$moduleName'")

        return PageScaffoldResult(
            pageName = template.pageName,
            moduleName = moduleName,
            files = previewFiles,
            dryRun = false,
        )
    }

    private fun kotlinSourceRootRelative(
        moduleDir: File,
        projectType: String,
    ): String = when {
        moduleDir.resolve("src/commonMain/kotlin").isDirectory -> "src/commonMain/kotlin"
        projectType.uppercase() in setOf("KMP", "KMP_ANDROID") -> "src/commonMain/kotlin"
        else -> "src/main/kotlin"
    }

    private fun useKmpPageTemplates(
        config: EgsConfig,
        kotlinRootRel: String,
    ): Boolean {
        val t = config.projectType.uppercase()
        if (t in setOf("KMP", "KMP_ANDROID")) return true
        return kotlinRootRel == "src/commonMain/kotlin"
    }

    /**
     * [PageKotlinTemplateRenderer] emits under `presentation.screen.<page>`; Koin imports must match.
     * KMP projects always use screen. Pure Android: optional [androidPresentationLayoutOverride], else detect
     * `presentation/screen` vs `presentation/fragment`, defaulting to screen for greenfield modules.
     */
    private fun resolveUseScreenPresentationLayout(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
        kotlinRootRel: String,
        useKmpPageTemplates: Boolean,
        androidPresentationLayoutOverride: String?,
    ): Boolean {
        if (useKmpPageTemplates) return true
        when (androidPresentationLayoutOverride?.lowercase()?.trim()) {
            "screen" -> return true
            "fragment" -> return false
        }
        val pkgPath = modulePackage.replace(".", "/")
        val presentation = projectRoot.resolve("feature/$moduleName/$kotlinRootRel/$pkgPath/presentation")
        if (!presentation.isDirectory) return true
        val hasScreen = presentation.resolve("screen").isDirectory
        val hasFragment = presentation.resolve("fragment").isDirectory
        return when {
            hasScreen && !hasFragment -> true
            hasFragment && !hasScreen -> false
            hasScreen && hasFragment -> true
            else -> true
        }
    }

    private fun previewFiles(
        template: PageTemplate,
        projectRoot: File,
        kotlinRootRel: String,
        useKmpPageTemplates: Boolean,
        withViewModelTest: Boolean,
    ): List<GeneratedFileInfo> {
        val modulePath = "feature/${template.moduleName}"
        val base =
            if (useKmpPageTemplates) {
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
            }.toMutableList()
        if (withViewModelTest) {
            val testPath = viewModelTestPath(modulePath, kotlinRootRel, template.modulePackage, template.pageName, useKmpPageTemplates)
            base.add(GeneratedFileInfo(testPath, "// ViewModel test placeholder\n"))
        }
        return base
    }

    private fun viewModelTestPath(
        modulePath: String,
        kotlinRootRel: String,
        modulePackage: String,
        pageName: String,
        useKmp: Boolean,
    ): String {
        val camel = pageName.replaceFirstChar { it.lowercase() }
        val pascal = pageName.replaceFirstChar { it.uppercase() }
        val testRoot = if (useKmp) "src/commonTest/kotlin" else "src/test/kotlin"
        val pkgPath = modulePackage.replace(".", "/")
        return "$modulePath/$testRoot/$pkgPath/presentation/screen/$camel/${pascal}ViewModelTest.kt"
    }

    private fun writeViewModelTest(
        projectRoot: File,
        moduleName: String,
        modulePackage: String,
        kotlinRootRel: String,
        pascalName: String,
        useKmp: Boolean,
    ) {
        val camel = pascalName.replaceFirstChar { it.lowercase() }
        val pkgPath = modulePackage.replace(".", "/")
        val testRoot =
            projectRoot.resolve(
                "feature/$moduleName/${if (useKmp) "src/commonTest/kotlin" else "src/test/kotlin"}/$pkgPath/presentation/screen/$camel",
            )
        val f = testRoot.resolve("${pascalName}ViewModelTest.kt")
        if (f.exists()) return
        testRoot.mkdirs()
        f.writeText(
            """
            package $modulePackage.presentation.screen.$camel

            import kotlin.test.Test

            class ${pascalName}ViewModelTest {
                @Test
                fun smoke_placeholder() {
                    // Wire fakes + turbine if needed
                }
            }
            """.trimIndent(),
        )
    }
}
