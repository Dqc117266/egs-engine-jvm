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

class CreateCommandsIntegrationTest {
    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `create module generates NavigationRoute and no XML resources`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "uiStructureEngine",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // 验证：应该生成 NavigationRoute
        assertTrue(projectRoot.resolve(
            "feature/uiStructureEngine/src/main/kotlin/com/dqc/example/feature/uiStructureEngine/presentation/UiStructureEngineNavigationRoute.kt"
        ).exists())

        // 验证：不应该生成 XML 文件
        assertFalse(projectRoot.resolve("feature/uiStructureEngine/src/main/res/layout/fragment_ui_structure_engine.xml").exists())
        assertFalse(projectRoot.resolve("feature/uiStructureEngine/src/main/res/navigation/ui_structure_engine_nav_graph.xml").exists())
    }

    @Test
    fun `create module dry run does not write files`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "dryRunFeature",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        assertFalse(projectRoot.resolve("feature/dryRunFeature").exists())
    }

    @Test
    fun `create page generates files in command mode`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()
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

        val vmPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/presentation/screen/tasklist/TaskListViewModel.kt",
        )
        assertTrue(vmPath.exists())
        val vmContent = vmPath.readText()
        assertTrue(vmContent.contains("handleTopicUpdateTopic("))
    }

    @Test
    fun `create page dry run does not write files`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()
        createUseCaseFixture(projectRoot)

        CreateCommand.withSubcommands().main(
            listOf(
                "page",
                "--module",
                "task",
                "--name",
                "TaskDryRun",
                "--api",
                "TopicUpdateTopic",
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        assertFalse(
            projectRoot.resolve(
                "feature/task/src/main/kotlin/com/dqc/example/feature/task/presentation/screen/taskdryrun",
            ).exists(),
        )
    }

    @Test
    fun `create api generates domain request body and toData call`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()
        val swaggerFile = createSwaggerFixture(projectRoot)

        CreateCommand.withSubcommands().main(
            listOf(
                "api",
                "task",
                "--swagger",
                swaggerFile.absolutePath,
                "--project",
                projectRoot.absolutePath,
            ),
        )

        val repositoryPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/domain/repository/TaskRepository.kt",
        )
        val repositoryImplPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/data/repository/TaskRepositoryImpl.kt",
        )
        val useCasePath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/domain/usecase/TopicUpdateTopicUseCase.kt",
        )

        assertTrue(repositoryPath.exists())
        assertTrue(repositoryImplPath.exists())
        assertTrue(useCasePath.exists())

        assertTrue(repositoryPath.readText().contains("suspend fun topicUpdateTopic(body: TopicSaveReqVO): Result<Boolean>"))
        assertTrue(repositoryImplPath.readText().contains("service.topicUpdateTopic(body.toData()).toResult()"))
        assertTrue(useCasePath.readText().contains("suspend operator fun invoke(body: TopicSaveReqVO): Result<Boolean>"))
    }

    @Test
    fun `create api dry run does not write files`() {
        startKoin { modules(featureScaffoldModule) }
        val projectRoot = createProjectFixture()
        val swaggerFile = createSwaggerFixture(projectRoot)

        CreateCommand.withSubcommands().main(
            listOf(
                "api",
                "task",
                "--swagger",
                swaggerFile.absolutePath,
                "--project",
                projectRoot.absolutePath,
                "--dry-run",
            ),
        )

        assertFalse(
            projectRoot.resolve(
                "feature/task/src/main/kotlin/com/dqc/example/feature/task/domain/repository/TaskRepository.kt",
            ).exists(),
        )
    }

    private fun createProjectFixture(): File {
        val root = kotlin.io.path.createTempDirectory("create-command-int-test").toFile()
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
                ":feature:common"
            )
            """.trimIndent(),
        )
        return root
    }

    private fun createUseCaseFixture(projectRoot: File) {
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

    private fun createSwaggerFixture(projectRoot: File): File {
        val swaggerFile = projectRoot.resolve("swagger.json")
        swaggerFile.writeText(
            """
            {
              "openapi": "3.0.1",
              "paths": {
                "/admin-api/ai/topic/update": {
                  "put": {
                    "operationId": "topicUpdateTopic",
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": { "${'$'}ref": "#/components/schemas/TopicSaveReqVO" }
                        }
                      }
                    },
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultBoolean" }
                          }
                        }
                      }
                    }
                  }
                }
              },
              "components": {
                "schemas": {
                  "TopicSaveReqVO": {
                    "type": "object",
                    "required": ["id", "name"],
                    "properties": {
                      "id": { "type": "integer", "format": "int64" },
                      "name": { "type": "string" }
                    }
                  },
                  "CommonResultBoolean": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": { "type": "boolean" }
                    }
                  }
                }
              }
            }
            """.trimIndent(),
        )
        return swaggerFile
    }
}
