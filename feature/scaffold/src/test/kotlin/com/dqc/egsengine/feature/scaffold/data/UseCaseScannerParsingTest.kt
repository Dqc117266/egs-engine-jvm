package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UseCaseScannerParsingTest {
    @TempDir
    lateinit var projectRoot: File

    @Test
    fun `parses multiline invoke and nested Result return type`() {
        val ucFile = projectRoot.resolve("feature/testmod/src/main/kotlin/com/foo/AppAiChatGetSessionPageUseCase.kt")
        ucFile.parentFile.mkdirs()
        ucFile.writeText(
            """
            package com.foo

            class AppAiChatGetSessionPageUseCase {
                suspend operator fun invoke(
                    topicId: Long?,
                    pageNo: Int,
                    pageSize: Int,
                ): Result<PageResult<SessionVo>> = Result.Success(PageResult())
            }
            """.trimIndent(),
        )

        val scanner = UseCaseScanner()
        val list = scanner.scanByModule(projectRoot, "testmod")
        val uc = list.single { it.name == "AppAiChatGetSessionPageUseCase" }
        assertEquals("Result<PageResult<SessionVo>>", uc.returnType)
        assertEquals(3, uc.parameters.size)
        assertEquals("Long?", uc.parameters[0].type)
        assertEquals("Int", uc.parameters[1].type)
        assertEquals("Int", uc.parameters[2].type)
    }

    @Test
    fun `parses block body invoke returning Result PageResultAppAiChatSessionRespVO`() {
        val ucFile = projectRoot.resolve("feature/blockmod/src/main/kotlin/com/foo/AppAiChatGetSessionPageUseCase.kt")
        ucFile.parentFile.mkdirs()
        ucFile.writeText(
            """
            package com.foo

            class AppAiChatGetSessionPageUseCase(private val repository: TodolistRepository) {
                suspend operator fun invoke(
                    topicId: Long?,
                    pageNo: Int,
                    pageSize: Int
                ): Result<PageResultAppAiChatSessionRespVO> {
                    return repository.appAiChatGetSessionPage(topicId, pageNo, pageSize)
                }
            }
            """.trimIndent(),
        )

        val scanner = UseCaseScanner()
        val list = scanner.scanByModule(projectRoot, "blockmod")
        val uc = list.single { it.name == "AppAiChatGetSessionPageUseCase" }
        assertEquals("Result<PageResultAppAiChatSessionRespVO>", uc.returnType)
        assertEquals(3, uc.parameters.size)
    }

    @Test
    fun `parses AppAiChatGetSessionPage style with imports and internal class`() {
        val ucFile =
            projectRoot.resolve(
                "feature/todolist/src/main/kotlin/com/example/feature/todolist/domain/usecase/AppAiChatGetSessionPageUseCase.kt",
            )
        ucFile.parentFile.mkdirs()
        ucFile.writeText(
            """
            package com.example.feature.todolist.domain.usecase

            import com.example.feature.base.domain.result.Result
            import com.example.feature.todolist.domain.model.PageResultAppAiChatSessionRespVO
            import com.example.feature.todolist.domain.repository.TodolistRepository
            import kotlin.Int
            import kotlin.Long

            internal class AppAiChatGetSessionPageUseCase(
              private val repository: TodolistRepository,
            ) {
              suspend operator fun invoke(
                topicId: Long?,
                pageNo: Int,
                pageSize: Int,
              ): Result<PageResultAppAiChatSessionRespVO> = repository.appAiChatGetSessionPage(topicId, pageNo, pageSize)
            }
            """.trimIndent(),
        )

        val scanner = UseCaseScanner()
        val list = scanner.scanByModule(projectRoot, "todolist")
        val uc = list.single { it.name == "AppAiChatGetSessionPageUseCase" }
        assertEquals("Result<PageResultAppAiChatSessionRespVO>", uc.returnType)
        assertEquals(3, uc.parameters.size)
    }

    @Test
    fun `enrichReturnTypesIfMissing fills return type when null`() {
        val ucFile = projectRoot.resolve("feature/enrichmod/src/main/kotlin/com/foo/EnrichUseCase.kt")
        ucFile.parentFile.mkdirs()
        ucFile.writeText(
            """
            package com.foo

            class EnrichUseCase {
                operator fun invoke(): Result<String> = Result.Success("")
            }
            """.trimIndent(),
        )

        val scanner = UseCaseScanner()
        val stale =
            UseCaseInfo(
                name = "EnrichUseCase",
                packageName = "com.foo",
                path = "feature/enrichmod/src/main/kotlin/com/foo/EnrichUseCase.kt",
                returnType = null,
                parameters = emptyList(),
            )
        val out = scanner.enrichReturnTypesIfMissing(projectRoot, listOf(stale))
        assertEquals("Result<String>", out.single().returnType)
    }

    @Test
    fun `parses invoke with function type parameter`() {
        val ucFile = projectRoot.resolve("feature/testmod2/src/main/kotlin/com/foo/BarUseCase.kt")
        ucFile.parentFile.mkdirs()
        ucFile.writeText(
            """
            package com.foo

            class BarUseCase {
                operator fun invoke(block: () -> Unit): Result<String> = Result.Success("")
            }
            """.trimIndent(),
        )

        val scanner = UseCaseScanner()
        val list = scanner.scanByModule(projectRoot, "testmod2")
        val uc = list.single()
        assertEquals("Result<String>", uc.returnType)
        assertEquals(1, uc.parameters.size)
        assertEquals("() -> Unit", uc.parameters[0].type)
    }
}
