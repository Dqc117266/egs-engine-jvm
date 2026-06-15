package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.init.di.featureInitModule
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
 * Integration tests after Compose UI migration:
 * 1. create module generates NavigationRoute, not XML layout/nav graph
 * 2. create page generates Compose screen, not fragment/XML layout
 */
class ComposeMigrationIntegrationTest {
    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `create module generates NavigationRoute and no XML layouts`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "testModule",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // Expect NavigationRoute
        val navRoutePath =
            projectRoot.resolve(
                "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/TestModuleNavigationRoute.kt",
            )
        assertTrue(navRoutePath.exists(), "expected NavigationRoute file")
        val navRouteContent = navRoutePath.readText()
        assertTrue(navRouteContent.contains("sealed interface TestModuleNavigationRoute"), "NavigationRoute should be sealed interface")
        assertTrue(navRouteContent.contains("@Serializable"), "NavigationRoute should have @Serializable")
        assertTrue(navRouteContent.contains("object TestModule"), "should contain default route object")

        // Expect no XML layout
        val layoutPath = projectRoot.resolve("feature/testModule/src/main/res/layout/fragment_test_module.xml")
        assertFalse(layoutPath.exists(), "expected no XML layout file")

        // Expect no NavGraph XML
        val navGraphPath = projectRoot.resolve("feature/testModule/src/main/res/navigation/test_module_nav_graph.xml")
        assertFalse(navGraphPath.exists(), "expected no NavGraph XML file")

        // Expect no Fragment
        val fragmentPath =
            projectRoot.resolve(
                "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/screen/TestModuleFragment.kt",
            )
        assertFalse(fragmentPath.exists(), "expected no Fragment file")

        // Expect other required files
        assertTrue(
            projectRoot
                .resolve(
                    "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/screen/TestModuleContract.kt",
                ).exists(),
            "expected Contract",
        )
        assertTrue(
            projectRoot
                .resolve(
                    "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/screen/TestModuleViewModel.kt",
                ).exists(),
            "expected ViewModel",
        )
    }

    @Test
    fun `create page generates Compose Screen and no Fragment or XML Layout`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        // Create module first
        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "home",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // Then create page
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

        // Expect Compose Screen
        val screenPath =
            projectRoot.resolve(
                "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeScreen.kt",
            )
        assertTrue(screenPath.exists(), "expected Compose Screen file")
        val screenContent = screenPath.readText()
        assertTrue(screenContent.contains("@Composable"), "Screen should have @Composable")
        assertTrue(screenContent.contains("fun HomeScreen("), "should define HomeScreen")
        assertTrue(screenContent.contains("koinViewModel()"), "should use koinViewModel")
        assertTrue(screenContent.contains("collectAsStateWithLifecycle()"), "should use collectAsStateWithLifecycle")
        assertTrue(screenContent.contains("LaunchedEffect(Unit)"), "should use LaunchedEffect for effects")
        assertTrue(screenContent.contains("Column("), "should use Column layout")
        assertTrue(screenContent.contains("when {"), "should use when for loading/error/content")

        // Expect no Fragment
        val fragmentPath =
            projectRoot.resolve(
                "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeFragment.kt",
            )
        assertFalse(fragmentPath.exists(), "expected no Fragment file")

        // Expect no XML layout
        val layoutPath = projectRoot.resolve("feature/home/src/main/res/layout/fragment_home.xml")
        assertFalse(layoutPath.exists(), "expected no XML layout file")

        // Expect Contract and ViewModel
        assertTrue(
            projectRoot
                .resolve(
                    "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeContract.kt",
                ).exists(),
            "expected Contract",
        )
        assertTrue(
            projectRoot
                .resolve(
                    "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/home/HomeViewModel.kt",
                ).exists(),
            "expected ViewModel",
        )
    }

    @Test
    fun `create page with use cases generates proper Screen with state handling`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "task",
                "--project",
                projectRoot.absolutePath,
            ),
        )
        createUseCaseFixture(projectRoot)

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

        val screenPath =
            projectRoot.resolve(
                "feature/task/src/main/kotlin/com/example/feature/task/presentation/screen/tasklist/TaskListScreen.kt",
            )
        assertTrue(screenPath.exists(), "expected Compose Screen file")

        val vmPath =
            projectRoot.resolve(
                "feature/task/src/main/kotlin/com/example/feature/task/presentation/screen/tasklist/TaskListViewModel.kt",
            )
        assertTrue(vmPath.exists())
        val vmContent = vmPath.readText()
        assertTrue(vmContent.contains("handleTopicUpdateTopic("))
        assertTrue(vmContent.contains("is Result.Success"), "expected Result-based handler")
        assertFalse(
            vmContent.contains("// TODO: map result to State"),
            "expected no placeholder Result TODO",
        )

        assertFalse(
            projectRoot
                .resolve(
                    "feature/task/src/main/kotlin/com/example/feature/task/presentation/screen/tasklist/TaskListFragment.kt",
                ).exists(),
        )
        assertFalse(projectRoot.resolve("feature/task/src/main/res/layout/fragment_task_list.xml").exists())
    }

    private fun createProjectFixture(): File {
        val root =
            kotlin.io.path
                .createTempDirectory("compose-migration-test")
                .toFile()
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
        val useCaseFile =
            projectRoot.resolve(
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
