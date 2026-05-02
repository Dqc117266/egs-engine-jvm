package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory

class SpringBootAppDependencyUpdaterTest {

    private val updater = SpringBootAppDependencyUpdater()

    @Test
    fun `ensureFeatureDependency appends feature dependency after existing feature dependencies`() {
        val backendRoot = createTempDirectory("springboot-app-deps").toFile()
        val buildFile = backendRoot.resolve("app/build.gradle.kts")
        buildFile.parentFile.mkdirs()
        buildFile.writeText(
            """
            plugins {
                id("com.egs.server.convention.application")
            }

            dependencies {
                implementation(project(":shared:infrastructure-oss"))
                implementation(project(":feature:example"))
                implementation(project(":feature:demo"))

                testImplementation(platform(libs.testcontainers.bom))
            }
            """.trimIndent(),
        )

        val changed = updater.ensureFeatureDependency(backendRoot, "recipe")

        assertTrue(changed)
        val text = buildFile.readText()
        assertTrue(text.contains("implementation(project(\":feature:recipe\"))"))
        assertTrue(
            text.indexOf("implementation(project(\":feature:recipe\"))") >
                text.indexOf("implementation(project(\":feature:demo\"))"),
        )
    }

    @Test
    fun `ensureFeatureDependency inserts first feature dependency when none exist`() {
        val backendRoot = createTempDirectory("springboot-app-no-feature").toFile()
        val buildFile = backendRoot.resolve("app/build.gradle.kts")
        buildFile.parentFile.mkdirs()
        buildFile.writeText(
            """
            dependencies {
                implementation(project(":shared:infrastructure-oss"))

                testImplementation(platform(libs.testcontainers.bom))
            }
            """.trimIndent(),
        )

        val changed = updater.ensureFeatureDependency(backendRoot, "recipe")

        assertTrue(changed)
        val text = buildFile.readText()
        assertTrue(text.contains("implementation(project(\":feature:recipe\"))"))
        assertTrue(
            text.indexOf("implementation(project(\":feature:recipe\"))") <
                text.indexOf("testImplementation(platform(libs.testcontainers.bom))"),
        )
    }

    @Test
    fun `ensureFeatureDependency is idempotent`() {
        val backendRoot = createTempDirectory("springboot-app-idempotent").toFile()
        val buildFile = backendRoot.resolve("app/build.gradle.kts")
        buildFile.parentFile.mkdirs()
        buildFile.writeText(
            """
            dependencies {
                implementation(project(":feature:recipe"))
            }
            """.trimIndent(),
        )

        val changed = updater.ensureFeatureDependency(backendRoot, "recipe")

        assertFalse(changed)
        assertEquals(1, buildFile.readText().lines().count { it.contains("implementation(project(\":feature:recipe\"))") })
    }
}
