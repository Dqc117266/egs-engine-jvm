package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.SettingsGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
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
            generator = ModuleGenerator(),
            settingsUpdater = SettingsGradleUpdater(),
        )

        val result = scaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = "uiStructureEngine",
            customPackage = "com.dqc.example",
            dryRun = true,
        )

        assertTrue(result.dryRun)
        // 验证：应该生成 NavigationRoute，而不是 XML
        assertTrue(result.files.any { it.endsWith("UiStructureEngineNavigationRoute.kt") }, "应该生成 NavigationRoute")
        assertFalse(result.files.any { it.endsWith("fragment_ui_structure_engine.xml") }, "不应该生成 XML Layout")
        assertFalse(result.files.any { it.endsWith("ui_structure_engine_nav_graph.xml") }, "不应该生成 NavGraph XML")
        assertFalse(projectRoot.resolve("feature/uiStructureEngine").exists())
    }

    @Test
    fun `page dry run previews Screen and Contract without creating directories`() {
        val projectRoot = createProjectFixture()
        val pageScaffolder = PageScaffolder(
            configReader = EgsConfigReader(),
            useCaseScanner = UseCaseScanner(),
            diUpdater = FeatureDiUpdater(),
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
        // 验证：应该生成 Compose Screen 和 Contract，而不是 Fragment/XML
        assertTrue(result.files.any { it.path.endsWith("TaskDetailScreen.kt") }, "应该生成 Screen")
        assertTrue(result.files.any { it.path.endsWith("TaskDetailContract.kt") }, "应该生成 Contract")
        assertFalse(result.files.any { it.path.endsWith("fragment_task_detail.xml") }, "不应该生成 XML Layout")
        assertFalse(
            projectRoot.resolve(
                "feature/task/src/main/kotlin/com/dqc/example/feature/task/presentation/screen/taskdetail",
            ).exists(),
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
}
