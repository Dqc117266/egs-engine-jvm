package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.github.ajalt.clikt.core.main
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class ClientListUsecasesCommandTest {
    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `client list usecases prints grouped use case names and paths`() {
        val root = createClientProjectWithUseCase()
        startKoin {
            modules(
                module {
                    single { UseCaseScanner() }
                },
            )
        }

        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            ClientCommand.withSubcommands().main(
                listOf("list", "usecases", "--project", root.absolutePath),
            )
        } finally {
            System.setOut(prev)
        }

        val text = out.toString()
        assertTrue(text.contains("feature/demo"), "expected module header\n$text")
        assertTrue(text.contains("FooUseCase"), "expected class name\n$text")
        assertTrue(!text.contains("FooUseCase.kt"), "should not print file paths\n$text")
    }

    @Test
    fun `client list usecases with module filters to one module`() {
        val root = createClientProjectWithUseCase()
        startKoin {
            modules(
                module {
                    single { UseCaseScanner() }
                },
            )
        }

        val out = ByteArrayOutputStream()
        val prev = System.out
        System.setOut(PrintStream(out))
        try {
            ClientCommand.withSubcommands().main(
                listOf("list", "usecases", "-m", "demo", "--project", root.absolutePath),
            )
        } finally {
            System.setOut(prev)
        }

        val text = out.toString()
        assertTrue(text.contains("FooUseCase"), text)
        assertTrue(text.contains("feature/demo"), text)
    }

    private fun createClientProjectWithUseCase(): File {
        val root =
            kotlin.io.path
                .createTempDirectory("client-list-usecases-test")
                .toFile()
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "list-usecases-test"
            """.trimIndent(),
        )
        val uc =
            root.resolve(
                "feature/demo/src/commonMain/kotlin/com/example/feature/demo/domain/usecase/FooUseCase.kt",
            )
        uc.parentFile.mkdirs()
        uc.writeText(
            """
            package com.example.feature.demo.domain.usecase

            class FooUseCase {
                suspend operator fun invoke(): Boolean = true
            }
            """.trimIndent(),
        )
        return root
    }
}
