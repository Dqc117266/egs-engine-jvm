package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.ViewModelEditScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.core.main
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ClientEditViewModelCommandUnitTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `client edit viewmodel maps page module usecases paging and dry-run to scaffolder`() {
        val scaffolder = mockk<ViewModelEditScaffolder>()
        val scanner = mockk<UseCaseScanner>()
        val logout =
            UseCaseInfo(
                name = "LogoutUseCase",
                packageName = "com.example.feature.user.domain",
                path = "feature/user/x/LogoutUseCase.kt",
                returnType = "template.core.base.network.domain.Result<Unit>",
            )
        every { scanner.listModules(any()) } returns listOf("user")
        every { scanner.scanByModule(any(), "user") } returns listOf(logout)
        every {
            scaffolder.edit(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            ViewModelEditScaffolder.ViewModelEditResult(
                pageScaffoldResult =
                    com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult(
                        pageName = "Profile",
                        moduleName = "user",
                        files = emptyList(),
                        dryRun = true,
                    ),
                stats =
                    ViewModelEditScaffolder.EditStats(
                        ctorParams = 0,
                        intents = 0,
                        stateFields = 0,
                        registerBlocks = 0,
                        handlers = 0,
                        contractImports = 0,
                        viewModelImports = 0,
                    ),
                diffs = emptyMap(),
            )

        startKoin {
            modules(
                module {
                    single { scaffolder }
                    single { scanner }
                },
            )
        }

        val projectRoot = kotlin.io.path.createTempDirectory("egs-edit-vm-cli").toFile()
        projectRoot.resolve("settings.gradle.kts").writeText("""rootProject.name = "unit-test"""")
        projectRoot.resolve(".egs").mkdirs()
        projectRoot.resolve(".egs/config.json").writeText(
            """{"projectType":"KMP","basePackage":"com.example"}""",
        )
        projectRoot.resolve("feature/user").mkdirs()

        ClientCommand.withSubcommands().main(
            listOf(
                "edit",
                "viewmodel",
                "Profile",
                "-m",
                "user",
                "-u",
                "LogoutUseCase",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
                "--paging",
                "none",
            ),
        )

        verify(exactly = 1) {
            scaffolder.edit(
                projectRoot = any(),
                moduleName = "user",
                pageName = "Profile",
                selectedUseCases = match { it.single().name == "LogoutUseCase" },
                dryRun = true,
                workspaceRoot = any(),
                pagingOption = "none",
            )
        }
    }

    @Test
    fun `space separated use cases after single -u are collected`() {
        val scaffolder = mockk<ViewModelEditScaffolder>()
        val scanner = mockk<UseCaseScanner>()
        val a =
            UseCaseInfo(
                name = "FirstUseCase",
                packageName = "com.example.feature.user.domain",
                path = "feature/user/x/FirstUseCase.kt",
                returnType = "template.core.base.network.domain.Result<Unit>",
            )
        val b =
            UseCaseInfo(
                name = "SecondUseCase",
                packageName = "com.example.feature.user.domain",
                path = "feature/user/x/SecondUseCase.kt",
                returnType = "template.core.base.network.domain.Result<Unit>",
            )
        every { scanner.listModules(any()) } returns listOf("user")
        every { scanner.scanByModule(any(), "user") } returns listOf(a, b)
        every {
            scaffolder.edit(any(), any(), any(), any(), any(), any(), any())
        } returns
            ViewModelEditScaffolder.ViewModelEditResult(
                pageScaffoldResult =
                    com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult(
                        pageName = "Profile",
                        moduleName = "user",
                        files = emptyList(),
                        dryRun = true,
                    ),
                stats =
                    ViewModelEditScaffolder.EditStats(
                        ctorParams = 0,
                        intents = 0,
                        stateFields = 0,
                        registerBlocks = 0,
                        handlers = 0,
                        contractImports = 0,
                        viewModelImports = 0,
                    ),
                diffs = emptyMap(),
            )

        startKoin {
            modules(
                module {
                    single { scaffolder }
                    single { scanner }
                },
            )
        }

        val projectRoot = kotlin.io.path.createTempDirectory("egs-edit-vm-cli-spaces").toFile()
        projectRoot.resolve("settings.gradle.kts").writeText("""rootProject.name = "unit-test"""")
        projectRoot.resolve(".egs").mkdirs()
        projectRoot.resolve(".egs/config.json").writeText(
            """{"projectType":"KMP","basePackage":"com.example"}""",
        )
        projectRoot.resolve("feature/user").mkdirs()

        ClientCommand.withSubcommands().main(
            listOf(
                "edit",
                "viewmodel",
                "Profile",
                "-m",
                "user",
                "-u",
                "FirstUseCase",
                "SecondUseCase",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
                "--paging",
                "none",
            ),
        )

        verify(exactly = 1) {
            scaffolder.edit(
                projectRoot = any(),
                moduleName = "user",
                pageName = "Profile",
                selectedUseCases = match { uc ->
                    uc.map { it.name } == listOf("FirstUseCase", "SecondUseCase")
                },
                dryRun = true,
                workspaceRoot = any(),
                pagingOption = "none",
            )
        }
    }
}
