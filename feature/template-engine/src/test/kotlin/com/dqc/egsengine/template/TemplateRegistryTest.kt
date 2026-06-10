package com.dqc.egsengine.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TemplateRegistryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `project templates override bundled classpath`() {
        val projectRoot = tempDir.resolve("project").toFile()
        val templates = projectRoot.resolve(".egs/templates/android/module")
        templates.mkdirs()
        templates.resolve("ViewModel.kt.ftl").writeText("project-override")

        val registry = TemplateRegistry()
        val template = registry.getTemplate("android/module/ViewModel.kt.ftl", projectRoot)
        assertEquals("project-override", template.toString().trim())
    }

    @Test
    fun `resolveEnvTemplateRoot returns null when unset`() {
        if (System.getenv(TemplateRegistry.ENV_TEMPLATE_ROOT).isNullOrBlank()) {
            assertNull(TemplateRegistry.resolveEnvTemplateRoot())
        }
    }
}
