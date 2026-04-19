package com.dqc.egsengine.feature.scaffold.data.config

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class WorkspaceConfigResolverTest {

    private val reader = WorkspaceConfigReader()
    private val resolver = WorkspaceConfigResolver(reader)

    @Test
    fun `resolveSwaggerUrl single-arg joins baseUrl and docPath`() {
        val root = workspaceRoot(
            """
            {
              "name": "w",
              "version": "1",
              "projects": {
                "client": {
                  "platform": "KMP",
                  "path": "client",
                  "basePackage": "com.example"
                },
                "backend": {
                  "platform": "SPRING_BOOT",
                  "path": "backend",
                  "basePackage": "com.example"
                }
              },
              "swagger": {
                "baseUrl": "http://localhost:8080",
                "docPath": "/v3/api-docs"
              }
            }
            """.trimIndent(),
        )
        assertEquals("http://localhost:8080/v3/api-docs", resolver.resolveSwaggerUrl(root))
    }

    @Test
    fun `resolveSwaggerUrl with client module uses modules docPath`() {
        val root = workspaceRoot(
            """
            {
              "name": "w",
              "version": "1",
              "projects": {
                "client": {
                  "platform": "KMP",
                  "path": "client",
                  "basePackage": "com.example"
                },
                "backend": {
                  "platform": "SPRING_BOOT",
                  "path": "backend",
                  "basePackage": "com.example"
                }
              },
              "swagger": {
                "baseUrl": "http://localhost:8080",
                "docPath": "/v3/api-docs",
                "modules": {
                  "todolist": { "backendModule": "todo", "docPath": "/v3/api-docs/todo" }
                }
              }
            }
            """.trimIndent(),
        )
        assertEquals("http://localhost:8080/v3/api-docs/todo", resolver.resolveSwaggerUrl(root, "todolist"))
        assertEquals("http://localhost:8080/v3/api-docs", resolver.resolveSwaggerUrl(root, "other"))
    }

    @Test
    fun `resolveSwaggerUrl module url overrides base`() {
        val root = workspaceRoot(
            """
            {
              "name": "w",
              "version": "1",
              "projects": {
                "client": {
                  "platform": "KMP",
                  "path": "client",
                  "basePackage": "com.example"
                },
                "backend": {
                  "platform": "SPRING_BOOT",
                  "path": "backend",
                  "basePackage": "com.example"
                }
              },
              "swagger": {
                "baseUrl": "http://localhost:8080",
                "docPath": "/v3/api-docs",
                "modules": {
                  "a": { "url": "http://other:9090/v3/api-docs/custom" }
                }
              }
            }
            """.trimIndent(),
        )
        assertEquals("http://other:9090/v3/api-docs/custom", resolver.resolveSwaggerUrl(root, "a"))
    }

    private fun workspaceRoot(json: String): File {
        val dir = createTempDirectory("workspace-resolver-test").toFile()
        val egs = dir.resolve(".egs").also { it.mkdirs() }
        egs.resolve("workspace.json").writeText(json)
        return dir
    }
}
