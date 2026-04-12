package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
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

class ClientApiSyncCommandUnitTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `client api sync --module maps to both client and backend`() {
        val apiSync = mockk<ApiSyncScaffolder>()
        every {
            apiSync.syncClientApi(any(), any(), any(), any(), any())
        } returns ApiSyncScaffolder.ApiSyncResult(
            clientModule = "todo",
            backendModule = "todo",
            files = emptyList(),
            dryRun = true,
        )

        startKoin {
            modules(
                module {
                    single { apiSync }
                },
            )
        }

        val projectRoot = createTempProjectDir()
        ClientCommand.withSubcommands().main(
            listOf(
                "api",
                "sync",
                "--module",
                "todo",
                "--swagger",
                "http://stub/swagger.json",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            apiSync.syncClientApi(
                any(),
                "todo",
                "todo",
                "http://stub/swagger.json",
                true,
            )
        }
    }

    @Test
    fun `explicit client and backend override --module`() {
        val apiSync = mockk<ApiSyncScaffolder>()
        every {
            apiSync.syncClientApi(any(), any(), any(), any(), any())
        } returns ApiSyncScaffolder.ApiSyncResult(
            clientModule = "a",
            backendModule = "b",
            files = emptyList(),
            dryRun = true,
        )

        startKoin {
            modules(
                module {
                    single { apiSync }
                },
            )
        }

        val projectRoot = createTempProjectDir()
        ClientCommand.withSubcommands().main(
            listOf(
                "api",
                "sync",
                "--module",
                "ignored",
                "--client-module",
                "alpha",
                "--backend-module",
                "beta",
                "--swagger",
                "http://stub/swagger.json",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            apiSync.syncClientApi(
                any(),
                "alpha",
                "beta",
                "http://stub/swagger.json",
                true,
            )
        }
    }

    @Test
    fun `positional module works as shortcut`() {
        val apiSync = mockk<ApiSyncScaffolder>()
        every {
            apiSync.syncClientApi(any(), any(), any(), any(), any())
        } returns ApiSyncScaffolder.ApiSyncResult(
            clientModule = "todo",
            backendModule = "todo",
            files = emptyList(),
            dryRun = true,
        )

        startKoin {
            modules(
                module {
                    single { apiSync }
                },
            )
        }

        val projectRoot = createTempProjectDir()
        ClientCommand.withSubcommands().main(
            listOf(
                "api",
                "sync",
                "todo",
                "--swagger",
                "http://stub/swagger.json",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            apiSync.syncClientApi(
                any(),
                "todo",
                "todo",
                "http://stub/swagger.json",
                true,
            )
        }
    }

    private fun createTempProjectDir(): File =
        kotlin.io.path.createTempDirectory("client-api-sync-unit-test").toFile().also {
            it.resolve("settings.gradle.kts").writeText("""rootProject.name = "unit-test-project"""")
        }
}
