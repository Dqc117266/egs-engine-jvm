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

class CreateCommandsIntegrationTest {
    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `create module generates NavigationRoute and no XML resources`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val projectRoot = createProjectFixture()

        CreateCommand.withSubcommands().main(
            listOf(
                "module",
                "uiStructureEngine",
                "--project",
                projectRoot.absolutePath,
            ),
        )

        // Expect NavigationRoute to be generated
        assertTrue(
            projectRoot.resolve(
                "feature/uiStructureEngine/src/main/kotlin/com/dqc/example/feature/uiStructureEngine/presentation/UiStructureEngineNavigationRoute.kt"
            ).exists()
        )

        // Expect no XML files
        assertFalse(
            projectRoot.resolve("feature/uiStructureEngine/src/main/res/layout/fragment_ui_structure_engine.xml")
                .exists()
        )
        assertFalse(
            projectRoot.resolve("feature/uiStructureEngine/src/main/res/navigation/ui_structure_engine_nav_graph.xml")
                .exists()
        )
    }

    @Test
    fun `create module dry run does not write files`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
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
        startKoin { modules(featureInitModule, featureScaffoldModule) }
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
        startKoin { modules(featureInitModule, featureScaffoldModule) }
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
        startKoin { modules(featureInitModule, featureScaffoldModule) }
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

        val apiRepositoryPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/repository/TaskApiRepository.kt",
        )
        val combinedRepositoryPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/repository/TaskRepository.kt",
        )
        val repositoryImplPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/data/repository/GeneratedTaskApiRepositorySupport.kt",
        )
        val useCasePath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/usecase/TopicUpdateTopicUseCase.kt",
        )

        assertTrue(apiRepositoryPath.exists())
        assertTrue(combinedRepositoryPath.exists())
        assertTrue(repositoryImplPath.exists())
        assertTrue(useCasePath.exists())

        val apiRepositoryText = apiRepositoryPath.readText().replace("\\s+".toRegex(), " ")
        assertTrue(apiRepositoryText.contains("suspend fun topicUpdateTopic"))
        assertTrue(apiRepositoryText.contains("body:"))
        assertTrue(apiRepositoryText.contains("TopicSaveReqVO"))
        assertTrue(apiRepositoryText.contains("Result<") && apiRepositoryText.contains("Boolean"))

        val combinedText = combinedRepositoryPath.readText().replace("\\s+".toRegex(), " ")
        assertTrue(combinedText.contains("interface TaskRepository"))
        assertTrue(combinedText.contains("TaskApiRepository"))

        assertTrue(
            repositoryImplPath.readText()
                .contains("service.topicUpdateTopic(body.toData()).toResult()"),
        )

        val useCaseText = useCasePath.readText().replace("\\s+".toRegex(), " ")
        assertTrue(useCaseText.contains("suspend operator fun invoke"))
        assertTrue(useCaseText.contains("body:"))
        assertTrue(useCaseText.contains("TopicSaveReqVO"))
        assertTrue(useCaseText.contains("Result<") && useCaseText.contains("Boolean"))
    }

    @Test
    fun `create api derives descriptive names from weak operation ids`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val projectRoot = createProjectFixture()
        val swaggerFile = createWeakOperationSwaggerFixture(projectRoot)

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

        val servicePath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/data/datasource/api/service/TaskRetrofitService.kt",
        )
        val repositoryPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/repository/TaskApiRepository.kt",
        )
        val repositorySupportPath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/data/repository/GeneratedTaskApiRepositorySupport.kt",
        )
        val getUseCasePath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/usecase/StepGetStepUseCase.kt",
        )
        val pageUseCasePath = projectRoot.resolve(
            "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/usecase/StepGetStepPageUseCase.kt",
        )

        assertTrue(servicePath.exists())
        assertTrue(repositoryPath.exists())
        assertTrue(repositorySupportPath.exists())
        assertTrue(getUseCasePath.exists())
        assertTrue(pageUseCasePath.exists())

        val serviceText = servicePath.readText().replace("\\s+".toRegex(), " ")
        assertTrue(serviceText.contains("suspend fun stepGetStep("))
        assertTrue(serviceText.contains("suspend fun stepUpdateStep("))
        assertTrue(serviceText.contains("suspend fun categoryDeleteCategory("))
        assertTrue(serviceText.contains("suspend fun stepGetStepPage("))
        assertTrue(serviceText.contains("suspend fun stepGetStepList("))
        assertTrue(serviceText.contains("suspend fun stepCountStep("))
        assertTrue(serviceText.contains("suspend fun stepGetAllStep("))
        assertTrue(serviceText.contains("suspend fun stepGetNextStepNumber("))
        assertFalse(serviceText.contains("suspend fun getById("))
        assertFalse(serviceText.contains("suspend fun update1("))

        val repositoryText = repositoryPath.readText().replace("\\s+".toRegex(), " ")
        assertTrue(repositoryText.contains("suspend fun stepGetStep("))
        assertTrue(repositoryText.contains("suspend fun stepGetStepPage("))
        assertFalse(repositoryText.contains("suspend fun list1("))

        val repositorySupportText = repositorySupportPath.readText().replace("\\s+".toRegex(), " ")
        assertTrue(repositorySupportText.contains("service.stepGetStep(id).toResult"))
        assertTrue(repositorySupportText.contains("service.stepUpdateStep(id, body.toData()).toResult"))
        assertTrue(repositorySupportText.contains("service.stepGetStepPage(page, size).toResult"))
        assertFalse(repositorySupportText.contains("service.getById("))
        assertFalse(repositorySupportText.contains("service.update1("))
    }

    @Test
    fun `create api dry run does not write files`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
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
                "feature/task/src/main/kotlin/com/dqc/example/feature/task/generate/domain/repository/TaskRepository.kt",
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

    private fun createWeakOperationSwaggerFixture(projectRoot: File): File {
        val swaggerFile = projectRoot.resolve("weak-operation-swagger.json")
        swaggerFile.writeText(
            """
            {
              "openapi": "3.0.1",
              "paths": {
                "/api/steps/{id}": {
                  "get": {
                    "operationId": "getById",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true, "schema": { "type": "integer", "format": "int64" } }
                    ],
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultRecipeStepResponse" }
                          }
                        }
                      }
                    }
                  },
                  "put": {
                    "operationId": "update1",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true, "schema": { "type": "integer", "format": "int64" } }
                    ],
                    "requestBody": {
                      "content": {
                        "application/json": {
                          "schema": { "${'$'}ref": "#/components/schemas/UpdateRecipeStepRequest" }
                        }
                      }
                    },
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultRecipeStepResponse" }
                          }
                        }
                      }
                    }
                  }
                },
                "/api/categories/{id}": {
                  "delete": {
                    "operationId": "delete1",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true, "schema": { "type": "integer", "format": "int64" } }
                    ],
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
                },
                "/api/steps": {
                  "get": {
                    "operationId": "list1",
                    "parameters": [
                      { "name": "page", "in": "query", "schema": { "type": "integer" } },
                      { "name": "size", "in": "query", "schema": { "type": "integer" } }
                    ],
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultPageResultRecipeStepResponse" }
                          }
                        }
                      }
                    }
                  }
                },
                "/api/steps/list": {
                  "get": {
                    "operationId": "listPath1",
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultPageResultRecipeStepResponse" }
                          }
                        }
                      }
                    }
                  }
                },
                "/api/steps/count": {
                  "get": {
                    "operationId": "count1",
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultLong" }
                          }
                        }
                      }
                    }
                  }
                },
                "/api/steps/all": {
                  "get": {
                    "operationId": "all1",
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultListRecipeStepResponse" }
                          }
                        }
                      }
                    }
                  }
                },
                "/api/steps/next-step-number": {
                  "get": {
                    "responses": {
                      "200": {
                        "content": {
                          "application/json": {
                            "schema": { "${'$'}ref": "#/components/schemas/CommonResultInt" }
                          }
                        }
                      }
                    }
                  }
                }
              },
              "components": {
                "schemas": {
                  "UpdateRecipeStepRequest": {
                    "type": "object",
                    "properties": {
                      "name": { "type": "string" }
                    }
                  },
                  "RecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "id": { "type": "integer", "format": "int64" },
                      "name": { "type": "string" }
                    }
                  },
                  "PageResultRecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "list": {
                        "type": "array",
                        "items": { "${'$'}ref": "#/components/schemas/RecipeStepResponse" }
                      },
                      "total": { "type": "integer", "format": "int64" }
                    }
                  },
                  "CommonResultRecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": { "${'$'}ref": "#/components/schemas/RecipeStepResponse" }
                    }
                  },
                  "CommonResultPageResultRecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": { "${'$'}ref": "#/components/schemas/PageResultRecipeStepResponse" }
                    }
                  },
                  "CommonResultLong": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": { "type": "integer", "format": "int64" }
                    }
                  },
                  "CommonResultInt": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": { "type": "integer" }
                    }
                  },
                  "CommonResultBoolean": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": { "type": "boolean" }
                    }
                  },
                  "CommonResultListRecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "code": { "type": "integer" },
                      "msg": { "type": "string" },
                      "data": {
                        "type": "array",
                        "items": { "${'$'}ref": "#/components/schemas/RecipeStepResponse" }
                      }
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
