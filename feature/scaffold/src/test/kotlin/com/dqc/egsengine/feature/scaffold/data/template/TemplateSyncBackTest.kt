package com.dqc.egsengine.feature.scaffold.data.template

import com.dqc.egsengine.feature.scaffold.data.TemplateRenameRecipes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TemplateSyncBackTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `sync-back copies path and reverses project name`() {
        val from = tempDir.resolve("from").toFile()
        val to = tempDir.resolve("to").toFile()
        from.mkdirs()
        to.mkdirs()

        val coreBase = from.resolve("core-base/ui")
        coreBase.mkdirs()
        coreBase.resolve("README.md").writeText("demo-app core for demo-app-client")

        val result =
            TemplateSyncBack().sync(
                fromDir = from,
                toDir = to,
                recipe = TemplateRenameRecipes.KMP_CLIENT,
                fromProjectName = "demo-app-client",
                fromPackage = null,
                toProjectName = "egs-kmp-template",
                toPackage = null,
                pathFilters = listOf("core-base"),
                dryRun = false,
            )

        assertTrue(result.copiedFiles.any { it.contains("core-base") })
        val synced = to.resolve("core-base/ui/README.md").readText()
        assertEquals("demo-app core for egs-kmp-template", synced)
    }

    @Test
    fun `dry-run lists files without writing`() {
        val from = tempDir.resolve("from").toFile()
        val to = tempDir.resolve("to").toFile()
        from.mkdirs()
        to.mkdirs()
        from.resolve("note.txt").writeText("keep")

        val result =
            TemplateSyncBack().sync(
                fromDir = from,
                toDir = to,
                recipe = TemplateRenameRecipes.KMP_CLIENT,
                fromProjectName = "demo",
                fromPackage = null,
                toProjectName = "egs-kmp-template",
                toPackage = null,
                pathFilters = listOf("note.txt"),
                dryRun = true,
            )

        assertEquals(listOf("note.txt"), result.copiedFiles)
        assertTrue(!to.resolve("note.txt").exists())
    }
}
