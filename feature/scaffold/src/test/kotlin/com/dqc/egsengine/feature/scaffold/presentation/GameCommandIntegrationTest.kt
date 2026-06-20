package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.init.di.featureInitModule
import com.dqc.egsengine.feature.scaffold.di.featureScaffoldModule
import com.github.ajalt.clikt.core.main
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import java.nio.file.Path

/**
 * End-to-end CLI test for `egs game add` against a Godot project root.
 * Exercises the full Clikt wiring (GameCommand -> GameAdd*Command) + Koin graph.
 */
class GameCommandIntegrationTest {
    @TempDir
    lateinit var tmp: Path

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `game add enemy writes gd tscn and registers const`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val root = seedGodotProject()

        GameCommand.withSubcommands().main(
            listOf("enemy", "slime", "--project", root.absolutePath),
        )

        assertTrue(root.resolve("entities/enemies/slime.gd").exists())
        assertTrue(root.resolve("entities/enemies/slime.tscn").exists())
        assertFalse(root.resolve("entities/enemies/slime.gd.uid").exists())
        val registry = root.resolve("autoload/EntityRegistry.gd").readText()
        assertTrue(registry.contains("const Slime = preload(\"res://entities/enemies/slime.gd\")"))
    }

    @Test
    fun `game add enemy dry run does not write files`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val root = seedGodotProject()

        GameCommand.withSubcommands().main(
            listOf("enemy", "bat", "--project", root.absolutePath, "--dry-run"),
        )

        assertFalse(root.resolve("entities/enemies/bat.gd").exists())
        assertFalse(root.resolve("entities/enemies/bat.tscn").exists())
        // Registry placeholder still intact (untouched).
        assertTrue(
            root.resolve("autoload/EntityRegistry.gd").readText()
                .contains("# (egs game add inserts preload() consts here)"),
        )
    }

    private fun seedGodotProject(): File {
        val root = tmp.toFile()
        File(root, ".egs").mkdirs()
        File(root, ".egs/workspace.json").writeText(
            """
            {
              "name": "test-game",
              "version": "2",
              "projects": {
                "game": {
                  "platform": "GODOT",
                  "path": ".",
                  "basePackage": "",
                  "engine": "godot",
                  "gameTemplate": "base"
                }
              }
            }
            """.trimIndent(),
        )
        File(root, "entities/enemies/core").mkdirs()
        File(root, "autoload").mkdirs()
        File(root, "autoload/EntityRegistry.gd").writeText(
            """
            extends Node
            # === EGS-AUTOGEN-BEGIN ===
            # (egs game add inserts preload() consts here)
            # === EGS-AUTOGEN-END ===
            """.trimIndent(),
        )
        return root
    }
}
