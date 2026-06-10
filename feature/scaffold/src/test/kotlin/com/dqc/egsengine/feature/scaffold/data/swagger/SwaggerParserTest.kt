package com.dqc.egsengine.feature.scaffold.data.swagger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class SwaggerParserTest {
    private val parser = SwaggerParser()

    @Test
    fun `preserves strong descriptive operation ids`() {
        val spec =
            parseSwagger(
                paths =
                """
                    "/admin-api/ai/topic/get": {
                      "get": {
                        "operationId": "Topic_getTopic",
                        "responses": {
                          "200": {
                            "content": {
                              "application/json": {
                                "schema": { "type": "boolean" }
                              }
                            }
                          }
                        }
                      }
                    },
                    "/app-api/ai/chat/message/list": {
                      "get": {
                        "operationId": "AppAiChat_getMessageList",
                        "responses": {
                          "200": {
                            "content": {
                              "application/json": {
                                "schema": { "type": "boolean" }
                              }
                            }
                          }
                        }
                      }
                    }
                """.trimIndent(),
            )

        assertEquals(
            listOf("topicGetTopic", "appAiChatGetMessageList"),
            spec.operations.map { it.operationId },
        )
    }

    @Test
    fun `derives descriptive operation ids from weak names and paths`() {
        val spec =
            parseSwagger(
                paths =
                """
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
                                "schema": { "${'$'}ref": "#/components/schemas/RecipeStepResponse" }
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
                                "schema": { "${'$'}ref": "#/components/schemas/RecipeStepResponse" }
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
                                "schema": { "type": "boolean" }
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
                                "schema": { "${'$'}ref": "#/components/schemas/PageResultRecipeStepResponse" }
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
                                "schema": { "${'$'}ref": "#/components/schemas/PageResultRecipeStepResponse" }
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
                                "schema": { "type": "integer", "format": "int64" }
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
                                "schema": {
                                  "type": "array",
                                  "items": { "${'$'}ref": "#/components/schemas/RecipeStepResponse" }
                                }
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
                                "schema": { "type": "integer" }
                              }
                            }
                          }
                        }
                      }
                    }
                """.trimIndent(),
            )

        assertEquals(
            listOf(
                "stepGetStep",
                "stepUpdateStep",
                "categoryDeleteCategory",
                "stepGetStepPage",
                "stepGetStepList",
                "stepCountStep",
                "stepGetAllStep",
                "stepGetNextStepNumber",
            ),
            spec.operations.map { it.operationId },
        )
    }

    private fun parseSwagger(paths: String): SwaggerSpec {
        val file = File.createTempFile("swagger-parser-test", ".json")
        file.writeText(
            """
            {
              "openapi": "3.0.1",
              "paths": {
                $paths
              },
              "components": {
                "schemas": {
                  "RecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "id": { "type": "integer", "format": "int64" }
                    }
                  },
                  "UpdateRecipeStepRequest": {
                    "type": "object",
                    "properties": {
                      "name": { "type": "string" }
                    }
                  },
                  "PageResultRecipeStepResponse": {
                    "type": "object",
                    "properties": {
                      "list": {
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
        return file.useAsTempPath { parser.parse(it.absolutePath) }
    }

    private fun <T> File.useAsTempPath(block: (File) -> T): T = try {
        block(this)
    } finally {
        delete()
    }
}
