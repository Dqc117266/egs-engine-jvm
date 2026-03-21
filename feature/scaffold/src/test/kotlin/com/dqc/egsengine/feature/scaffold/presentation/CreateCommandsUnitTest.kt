package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import com.github.ajalt.clikt.core.main
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

class CreateCommandsUnitTest {
    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `create module maps args to scaffolder`() {
        val scaffolder = mockk<ModuleScaffolder>()
        every {
            scaffolder.scaffold(any(), any(), any(), any())
        } returns ModuleScaffolder.ScaffoldResult(
            moduleName = "task",
            files = listOf("feature/task/build.gradle.kts"),
            dryRun = true,
        )

        startKoin {
            modules(
                module {
                    single { scaffolder }
                },
            )
        }

        val projectRoot = createTempProjectDir()
        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "task",
                "--project",
                projectRoot.absolutePath,
                "--package",
                "com.example.demo",
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            scaffolder.scaffold(
                any(),
                "task",
                "com.example.demo",
                true,
            )
        }
    }

    @Test
    fun `create page command mode resolves usecases and calls scaffolder`() {
        val pageScaffolder = mockk<PageScaffolder>()
        val useCaseScanner = mockk<UseCaseScanner>()
        every { useCaseScanner.listModules(any()) } returns listOf("task")
        val topicUseCase = UseCaseInfo(
            name = "TopicUpdateTopicUseCase",
            packageName = "com.example.feature.task.domain.usecase",
            path = "feature/task/src/main/kotlin/TopicUpdateTopicUseCase.kt",
            returnType = "Result<Boolean>",
        )
        every { useCaseScanner.scanByModule(any(), "task") } returns listOf(topicUseCase)
        every {
            pageScaffolder.scaffold(any(), any(), any(), any(), any())
        } returns PageScaffoldResult(
            pageName = "TaskList",
            moduleName = "task",
            files = emptyList(),
            dryRun = true,
        )

        startKoin {
            modules(
                module {
                    single { pageScaffolder }
                    single { useCaseScanner }
                },
            )
        }

        val projectRoot = createTempProjectDir()
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
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            pageScaffolder.scaffold(
                any(),
                "task",
                "TaskList",
                listOf(topicUseCase),
                true,
            )
        }
    }

    @Test
    fun `create api maps args to swagger scaffolder`() {
        val swaggerScaffolder = mockk<SwaggerApiScaffolder>()
        every {
            swaggerScaffolder.scaffold(any(), any(), any(), any(), any())
        } returns SwaggerApiScaffolder.Result(
            files = listOf("feature/task/src/main/kotlin/com/example/task/domain/DomainModule.kt"),
            dryRun = true,
        )

        startKoin {
            modules(
                module {
                    single { swaggerScaffolder }
                },
            )
        }

        val projectRoot = createTempProjectDir()
        CreateCommand.withSubcommands().main(
            listOf(
                "api",
                "task",
                "--swagger",
                "/tmp/swagger.json",
                "--project",
                projectRoot.absolutePath,
                "--package",
                "com.example.demo",
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            swaggerScaffolder.scaffold(
                any(),
                "task",
                "/tmp/swagger.json",
                "com.example.demo",
                true,
            )
        }
    }

    private fun createTempProjectDir(): File =
        kotlin.io.path.createTempDirectory("create-command-unit-test").toFile().also {
            it.resolve("settings.gradle.kts").writeText("""rootProject.name = "unit-test-project"""")
        }
}
