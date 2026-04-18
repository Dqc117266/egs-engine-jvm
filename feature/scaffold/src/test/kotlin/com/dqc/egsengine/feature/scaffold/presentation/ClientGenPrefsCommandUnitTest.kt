package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.domain.ClientPrefsScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpPreferencesScaffolder
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

class ClientGenPrefsCommandUnitTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `client gen prefs passes module fields project dry-run and key`() {
        val scaffolder = mockk<ClientPrefsScaffolder>()
        every {
            scaffolder.scaffoldPrefs(any(), any(), any(), any(), any(), any())
        } returns KmpPreferencesScaffolder.KmpPreferencesScaffoldResult(
            moduleName = "todo",
            files = listOf(GeneratedFile("feature/todo/x.kt", "")),
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

        ClientCommand.withSubcommands().main(
            listOf(
                "gen",
                "prefs",
                "--module",
                "todo",
                "--fields",
                "userId:String",
                "--key",
                "user_id",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            scaffolder.scaffoldPrefs(
                match { it.canonicalPath == projectRoot.canonicalFile.canonicalPath },
                "todo",
                "userId:String",
                "user_id",
                true,
                false,
            )
        }
    }

    @Test
    fun `alias --feilds maps to fields`() {
        val scaffolder = mockk<ClientPrefsScaffolder>()
        every {
            scaffolder.scaffoldPrefs(any(), any(), any(), any(), any(), any())
        } returns KmpPreferencesScaffolder.KmpPreferencesScaffoldResult(
            moduleName = "m",
            files = emptyList(),
            dryRun = false,
        )

        startKoin {
            modules(
                module {
                    single { scaffolder }
                },
            )
        }

        val projectRoot = createTempProjectDir()

        ClientCommand.withSubcommands().main(
            listOf(
                "gen",
                "prefs",
                "-m",
                "m",
                "--feilds",
                "x:Int",
                "-p",
                projectRoot.absolutePath,
            ),
        )

        verify(exactly = 1) {
            scaffolder.scaffoldPrefs(any(), "m", "x:Int", null, false, false)
        }
    }

    private fun createTempProjectDir(): File =
        kotlin.io.path.createTempDirectory("client-gen-prefs-unit-test").toFile().also {
            it.resolve("settings.gradle.kts").writeText("""rootProject.name = "unit-test-project"""")
        }
}
