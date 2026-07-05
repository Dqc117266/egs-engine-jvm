package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.init.di.featureInitModule
import com.dqc.egsengine.feature.scaffold.di.featureScaffoldModule
import com.dqc.egsengine.feature.scaffold.golden.GodotTestProject
import com.github.ajalt.clikt.core.main
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.nio.file.Path

/**
 * End-to-end CLI test for `egs game add` against a Godot project root.
 * Exercises the full Clikt wiring (GameCommand -> GameAddGroupCommand -> GameAddEntityCommand) + Koin graph.
 */
class GameCommandIntegrationTest {
    @TempDir
    lateinit var tmp: Path

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `game add enemy dry run writes nothing`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val root = GodotTestProject.seed(tmp.toFile())

        GameCommand.withSubcommands().main(
            listOf("add", "enemy", "slime", "--project", root.absolutePath, "--dry-run"),
        )

        assertFalse(root.resolve("modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd").exists())
        assertFalse(root.resolve("modules/combat/generated/registry.gd").exists())
    }

    @Test
    fun `game add enemy writes modular files and registry`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val root = GodotTestProject.seed(tmp.toFile())

        GameCommand.withSubcommands().main(
            listOf("add", "enemy", "slime", "--project", root.absolutePath),
        )

        assertTrue(root.resolve("modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd").exists())
        assertTrue(root.resolve("modules/combat/src/entities/enemies/slime/Slime.gd").exists())
        assertTrue(root.resolve("modules/combat/scenes/enemies/slime.tscn").exists())
        val registry = root.resolve("modules/combat/generated/registry.gd").readText()
        assertTrue(registry.contains("const Slime = preload"))
    }

    @Test
    fun `game add item and ui skip the registry`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val root = GodotTestProject.seed(tmp.toFile())

        GameCommand.withSubcommands().main(listOf("add", "item", "health_potion", "--project", root.absolutePath))
        GameCommand.withSubcommands().main(listOf("add", "ui", "inventory_panel", "--project", root.absolutePath))

        assertTrue(root.resolve("modules/inventory/generated/resources/items/health_potion.tres").exists())
        assertTrue(root.resolve("modules/ui/src/InventoryPanel.gd").exists())
        assertFalse(root.resolve("modules/inventory/generated/registry.gd").exists())
    }

    @Test
    fun `game add module creates the skeleton`() {
        startKoin { modules(featureInitModule, featureScaffoldModule) }
        val root = GodotTestProject.seed(tmp.toFile())

        GameCommand.withSubcommands().main(listOf("add", "module", "fishing", "--project", root.absolutePath))

        assertTrue(root.resolve("modules/fishing/module.json").exists())
        assertTrue(root.resolve("modules/fishing/README.md").exists())
        listOf("api", "generated", "src", "scenes", "resources", "tests").forEach { sub ->
            assertTrue(root.resolve("modules/fishing/$sub/.gitkeep").exists())
        }
    }
}
