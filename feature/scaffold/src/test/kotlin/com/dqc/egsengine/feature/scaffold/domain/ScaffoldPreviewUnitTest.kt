package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.scaffold.data.ClientAppNavigationWiring
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScaffoldPreviewUnitTest {
    @Test
    fun `module dry run previews NavigationRoute without writing files`() {
        val projectRoot = createProjectFixture()
        val scaffolder = ModuleScaffolder(
            configReader = EgsConfigReader(),
            generator = ModuleGenerator(
                AndroidModuleGenerator(
                    settingsUpdater = SettingsGradleUpdater(),
                    templateEngine = TemplateEngine(TemplateRegistry()),
                ),
            ),
            settingsUpdater = SettingsGradleUpdater(),
            workspaceResolver = WorkspaceConfigResolver(WorkspaceConfigReader()),
            platformGenerators = emptyMap(),
        )

        val result = scaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = "uiStructureEngine",
            customPackage = "com.dqc.example",
            dryRun = true,
        )

        assertTrue(result.dryRun)
        // Expect NavigationRoute in the dry-run file list, not XML
        assertTrue(result.files.any { it.endsWith("UiStructureEngineNavigationRoute.kt") }, "expected NavigationRoute")
        assertFalse(result.files.any { it.endsWith("fragment_ui_structure_engine.xml") }, "expected no XML layout")
        assertFalse(result.files.any { it.endsWith("ui_structure_engine_nav_graph.xml") }, "expected no NavGraph XML")
        assertFalse(projectRoot.resolve("feature/uiStructureEngine").exists())
    }

    @Test
    fun `page dry run previews Screen and Contract without creating directories`() {
        val projectRoot = createProjectFixture()
        val pageScaffolder = PageScaffolder(
            configReader = EgsConfigReader(),
            useCaseScanner = UseCaseScanner(),
            diUpdater = FeatureDiUpdater(),
            templateEngine = TemplateEngine(TemplateRegistry()),
            clientAppNavigationWiring = ClientAppNavigationWiring(),
        )

        val result = pageScaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = "task",
            pageName = "TaskDetail",
            useCases = listOf(
                UseCaseInfo(
                    name = "TopicUpdateTopicUseCase",
                    packageName = "com.dqc.example.feature.task.domain.usecase",
                    path = "feature/task/src/main/kotlin/com/dqc/example/feature/task/domain/usecase/TopicUpdateTopicUseCase.kt",
                    returnType = "Result<Boolean>",
                ),
            ),
            dryRun = true,
        )

        assertTrue(result.dryRun)
        // Expect Compose Screen and Contract in the dry-run list, not Fragment/XML
        assertTrue(result.files.any { it.path.endsWith("TaskDetailScreen.kt") }, "expected Screen")
        assertTrue(result.files.any { it.path.endsWith("TaskDetailContract.kt") }, "expected Contract")
        assertFalse(result.files.any { it.path.endsWith("fragment_task_detail.xml") }, "expected no XML layout")
        assertFalse(
            projectRoot.resolve(
                "feature/task/src/main/kotlin/com/dqc/example/feature/task/presentation/screen/taskdetail",
            ).exists(),
        )
    }

    @Test
    fun `page dry run KMP uses presentation path and kmp templates`() {
        val projectRoot = createKmpProjectFixture()
        val pageScaffolder = PageScaffolder(
            configReader = EgsConfigReader(),
            useCaseScanner = UseCaseScanner(),
            diUpdater = FeatureDiUpdater(),
            templateEngine = TemplateEngine(TemplateRegistry()),
            clientAppNavigationWiring = ClientAppNavigationWiring(),
        )

        val result = pageScaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = "task",
            pageName = "TaskDetail",
            useCases = listOf(
                UseCaseInfo(
                    name = "TopicUpdateTopicUseCase",
                    packageName = "com.dqc.example.feature.task.domain.usecase",
                    path = "feature/task/src/commonMain/kotlin/com/dqc/example/feature/task/domain/usecase/TopicUpdateTopicUseCase.kt",
                    returnType = "template.core.base.network.domain.Result<Boolean>",
                ),
            ),
            dryRun = true,
        )

        assertTrue(result.dryRun)
        val paths = result.files.map { it.path }
        assertTrue(paths.any { it.contains("presentation/screen/taskDetail/TaskDetailScreen.kt") }, "expected KMP screen path")
        assertTrue(paths.any { it.contains("TaskDetailContract.kt") }, "expected Contract")
        val contractContent = result.files.first { it.path.endsWith("TaskDetailContract.kt") }.content
        assertTrue(contractContent.contains("com.dqc.example.core.base.ui.LoadableState"), "expected KMP contract imports")
        val vmContent = result.files.first { it.path.endsWith("TaskDetailViewModel.kt") }.content
        assertTrue(vmContent.contains("com.dqc.example.core.base.ui.BaseViewModel"), "expected KMP BaseViewModel")
        assertTrue(
            vmContent.contains("com.dqc.example.core.base.network.domain.Result"),
            "expected basePackage-derived core network Result import for KMP page",
        )
    }

    private fun createProjectFixture(): File {
        val root = kotlin.io.path.createTempDirectory("scaffold-preview-unit-test").toFile()
        root.resolve(".egs").mkdirs()
        root.resolve(".egs/config.json").writeText(
            """
            {
              "projectName": "fixture",
              "projectType": "ANDROID",
              "rootPath": "${root.absolutePath.replace("\\", "\\\\")}",
              "conventionPluginId": "com.dqc.example.convention.feature",
              "basePackage": "com.dqc.example",
              "moduleStructure": {
                "layers": ["data", "domain", "presentation"],
                "hasRes": true
              },
              "baseClasses": []
            }
            """.trimIndent(),
        )
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "fixture"
            include(
                ":feature:base",
                ":feature:common",
            )
            """.trimIndent(),
        )
        return root
    }

    private fun createKmpProjectFixture(): File {
        val root = kotlin.io.path.createTempDirectory("scaffold-preview-kmp-unit-test").toFile()
        root.resolve(".egs").mkdirs()
        root.resolve(".egs/config.json").writeText(
            """
            {
              "projectName": "fixture-kmp",
              "projectType": "KMP",
              "rootPath": "${root.absolutePath.replace("\\", "\\\\")}",
              "conventionPluginId": "com.dqc.example.convention.feature",
              "basePackage": "com.dqc.example",
              "moduleStructure": {
                "layers": ["data", "domain", "presentation"],
                "hasRes": false
              },
              "baseClasses": []
            }
            """.trimIndent(),
        )
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "fixture-kmp"
            include(
                ":feature:base",
                ":feature:common",
            )
            """.trimIndent(),
        )
        return root
    }
}
