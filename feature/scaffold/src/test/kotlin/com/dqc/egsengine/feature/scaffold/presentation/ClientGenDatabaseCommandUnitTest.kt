package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.domain.KmpDatabaseScaffolder
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

class ClientGenDatabaseCommandUnitTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `client gen database passes sql module project and dry-run`() {
        val scaffolder = mockk<KmpDatabaseScaffolder>()
        every {
            scaffolder.scaffoldDatabase(any(), any(), any(), any())
        } returns KmpDatabaseScaffolder.KmpDatabaseScaffoldResult(
            moduleName = "storage",
            files = listOf(GeneratedFile("feature/storage/x.kt", "")),
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
        val sqlFile = File.createTempFile("dbgen", ".sql").apply {
            writeText("CREATE TABLE t (id INT NOT NULL PRIMARY KEY);")
        }

        ClientCommand.withSubcommands().main(
            listOf(
                "gen",
                "database",
                sqlFile.absolutePath,
                "--module",
                "storage",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        verify(exactly = 1) {
            scaffolder.scaffoldDatabase(
                match { it.canonicalPath == projectRoot.canonicalFile.canonicalPath },
                match { it.canonicalPath == sqlFile.canonicalFile.canonicalPath },
                "storage",
                true,
            )
        }
    }

    @Test
    fun `short option -m works`() {
        val scaffolder = mockk<KmpDatabaseScaffolder>()
        every {
            scaffolder.scaffoldDatabase(any(), any(), any(), any())
        } returns KmpDatabaseScaffolder.KmpDatabaseScaffoldResult(
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
        val sqlFile = File.createTempFile("dbgen2", ".sql").apply {
            writeText("CREATE TABLE t (id INT NOT NULL PRIMARY KEY);")
        }

        ClientCommand.withSubcommands().main(
            listOf(
                "gen",
                "database",
                sqlFile.absolutePath,
                "-m",
                "m",
                "-p",
                projectRoot.absolutePath,
            ),
        )

        verify(exactly = 1) {
            scaffolder.scaffoldDatabase(any(), any(), "m", false)
        }
    }

    private fun createTempProjectDir(): File =
        kotlin.io.path.createTempDirectory("client-gen-db-unit-test").toFile().also {
            it.resolve("settings.gradle.kts").writeText("""rootProject.name = "unit-test-project"""")
        }
}
