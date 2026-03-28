package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.di.featureScaffoldModule
import com.github.ajalt.clikt.core.main
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File

/**
 * Compose UI 迁移后的集成测试
 * 验证：
 * 1. create module 生成 NavigationRoute，不生成 XML Layout/NavGraph
 * 2. create page 生成 Compose Screen，不生成 Fragment/XML Layout
 */
class ComposeMigrationIntegrationTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `create module generates NavigationRoute and no XML layouts`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "testModule",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // 验证：应该生成 NavigationRoute
        val navRoutePath = projectRoot.resolve(
            "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/TestModuleNavigationRoute.kt"
        )
        assertTrue(navRoutePath.exists(), "应该生成 NavigationRoute 文件")
        val navRouteContent = navRoutePath.readText()
        assertTrue(navRouteContent.contains("sealed interface TestModuleNavigationRoute"), "NavigationRoute 应该是密封接口")
        assertTrue(navRouteContent.contains("@Serializable"), "NavigationRoute 应该有 @Serializable 注解")
        assertTrue(navRouteContent.contains("object TestModule"), "应该包含默认路由对象")

        // 验证：不应该生成 XML Layout
        val layoutPath = projectRoot.resolve("feature/testModule/src/main/res/layout/fragment_test_module.xml")
        assertFalse(layoutPath.exists(), "不应该生成 XML Layout 文件")

        // 验证：不应该生成 NavGraph XML
        val navGraphPath = projectRoot.resolve("feature/testModule/src/main/res/navigation/test_module_nav_graph.xml")
        assertFalse(navGraphPath.exists(), "不应该生成 NavGraph XML 文件")

        // 验证：不应该生成 Fragment
        val fragmentPath = projectRoot.resolve(
            "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/screen/TestModuleFragment.kt"
        )
        assertFalse(fragmentPath.exists(), "不应该生成 Fragment 文件")

        // 验证：应该生成其他必要文件
        assertTrue(projectRoot.resolve(
            "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/screen/TestModuleContract.kt"
        ).exists(), "应该生成 Contract")
        assertTrue(projectRoot.resolve(
            "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/screen/TestModuleViewModel.kt"
        ).exists(), "应该生成 ViewModel")
    }

    @Test
    fun `create page generates Compose Screen and no Fragment or XML Layout`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        // 先创建模块
        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "home",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // 再创建页面
        CreateCommand.withSubcommands().main(
            listOf(
                "page",
                "--module",
                "home",
                "--name",
                "Home",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // 验证：应该生成 Compose Screen
        val screenPath = projectRoot.resolve(
            "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeScreen.kt"
        )
        assertTrue(screenPath.exists(), "应该生成 Compose Screen 文件")
        val screenContent = screenPath.readText()
        assertTrue(screenContent.contains("@Composable"), "Screen 应该有 @Composable 注解")
        assertTrue(screenContent.contains("fun HomeScreen("), "应该有 HomeScreen 函数")
        assertTrue(screenContent.contains("koinViewModel()"), "应该使用 koinViewModel 注入")
        assertTrue(screenContent.contains("collectAsStateWithLifecycle()"), "应该使用 collectAsStateWithLifecycle 收集状态")
        assertTrue(screenContent.contains("LaunchedEffect(Unit)"), "应该有 LaunchedEffect 处理 Effect")
        assertTrue(screenContent.contains("Column("), "应该有 Column 布局")
        assertTrue(screenContent.contains("when {"), "应该有 when 判断 loading/error/content")

        // 验证：不应该生成 Fragment
        val fragmentPath = projectRoot.resolve(
            "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeFragment.kt"
        )
        assertFalse(fragmentPath.exists(), "不应该生成 Fragment 文件")

        // 验证：不应该生成 XML Layout
        val layoutPath = projectRoot.resolve("feature/home/src/main/res/layout/fragment_home.xml")
        assertFalse(layoutPath.exists(), "不应该生成 XML Layout 文件")

        // 验证：应该生成 Contract 和 ViewModel
        assertTrue(projectRoot.resolve(
            "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeContract.kt"
        ).exists(), "应该生成 Contract")
        assertTrue(projectRoot.resolve(
            "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeViewModel.kt"
        ).exists(), "应该生成 ViewModel")
    }

    @Test
    fun `create page with use cases generates proper Screen with state handling`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()
        createUseCaseFixture(projectRoot)

        // 先创建模块
        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "task",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // 创建带有 UseCase 的页面
        CreateCommand.withSubcommands().main(
            listOf(
                "page",
                "--module",
                "task",
                "--name",
                "TaskList",
                "--api",
                "TopicUpdateTopic",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // 验证：应该生成 Compose Screen
        val screenPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/example/feature/task/presentation/screen/tasklist/TaskListScreen.kt"
        )
        assertTrue(screenPath.exists(), "应该生成 Compose Screen 文件")

        // 验证 ViewModel 包含 use case 处理
        val vmPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/example/feature/task/presentation/screen/tasklist/TaskListViewModel.kt",
        )
        assertTrue(vmPath.exists())
        val vmContent = vmPath.readText()
        assertTrue(vmContent.contains("handleTopicUpdateTopic("))

        // 验证：不应该生成 Fragment 和 Layout
        assertFalse(projectRoot.resolve(
            "feature/task/src/main/kotlin/com/example/feature/task/presentation/screen/tasklist/TaskListFragment.kt"
        ).exists())
        assertFalse(projectRoot.resolve("feature/task/src/main/res/layout/fragment_task_list.xml").exists())
    }

    private fun createProjectFixture(): File {
        val root = kotlin.io.path.createTempDirectory("compose-migration-test").toFile()
        root.resolve(".egs").mkdirs()
        root.resolve(".egs/config.json").writeText(
            """
            {
              "projectName": "fixture",
              "projectType": "ANDROID",
              "rootPath": "${root.absolutePath.replace("\\", "\\\\")}",
              "conventionPluginId": "com.dqc.example.convention.feature",
              "basePackage": "com.example",
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
                ":feature:common"
            )
            """.trimIndent(),
        )
        return root
    }

    private fun createUseCaseFixture(projectRoot: File) {
        // 先创建 task 模块目录结构
        val useCaseFile = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/domain/usecase/TopicUpdateTopicUseCase.kt",
        )
        useCaseFile.parentFile.mkdirs()
        useCaseFile.writeText(
            """
            package com.dqc.example.feature.task.domain.usecase

            import com.dqc.example.feature.base.domain.result.Result

            internal class TopicUpdateTopicUseCase {
              suspend operator fun invoke(topicId: Long): Result<Boolean> = Result.Success(true)
            }
            """.trimIndent(),
        )
    }
}
