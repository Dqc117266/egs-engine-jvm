package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityRegistryUpdater
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityKind
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GodotEntityScaffolderTest {
    @TempDir
    lateinit var tmp: Path

    private fun scaffolder() = GodotEntityScaffolder(
        WorkspaceConfigResolver(WorkspaceConfigReader()),
        GodotEntityGenerator(TemplateEngine(TemplateRegistry())),
        GodotEntityRegistryUpdater(),
    )

    private fun seedGameProject(gameTemplate: String = "base"): File {
        val root = tmp.toFile()
        // Minimal Godot workspace.json for the `game` sub-project.
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
                  "gameTemplate": "$gameTemplate"
                }
              }
            }
            """.trimIndent(),
        )
        // Base-class stubs the generated entity extends (so file presence checks are realistic).
        File(root, "entities/enemies/core").mkdirs()
        File(root, "entities/skills/core").mkdirs()
        File(root, "entities/rooms/core").mkdirs()
        // EntityRegistry with the autogen markers.
        File(root, "autoload").mkdirs()
        File(root, "autoload/EntityRegistry.gd").writeText(
            """
            extends Node
            # === EGS-AUTOGEN-BEGIN ===
            # (egs game add inserts preload() consts here)
            # === EGS-AUTOGEN-END ===

            func _ready() -> void:
                pass
            """.trimIndent(),
        )
        return root
    }

    @Test
    fun `dry run does not write files`() {
        val root = seedGameProject()
        val result = scaffolder().scaffold(root, GodotEntityKind.ENEMY, "slime", dryRun = true)

        result.dryRun `should be equal to` true
        result.className `should be equal to` "Slime"
        // No files created on disk.
        File(root, "entities/enemies/slime.gd").exists() `should be equal to` false
        File(root, "entities/enemies/slime.tscn").exists() `should be equal to` false
        // Registry untouched on dry-run.
        val registry = File(root, "autoload/EntityRegistry.gd").readText()
        registry `should contain` "# (egs game add inserts preload() consts here)"
    }

    @Test
    fun `writes gd and tscn and registers const for enemy`() {
        val root = seedGameProject()
        val result = scaffolder().scaffold(root, GodotEntityKind.ENEMY, "slime", dryRun = false)

        result.dryRun `should be equal to` false
        val script = File(root, "entities/enemies/slime.gd")
        script.exists() `should be equal to` true
        script.readText() `should contain` "class_name Slime"
        script.readText() `should contain` "extends \"res://entities/enemies/core/enemy.gd\""
        File(root, "entities/enemies/slime.tscn").exists() `should be equal to` true
        // No cache files produced.
        File(root, "entities/enemies/slime.gd.uid").exists() `should be equal to` false

        val registry = File(root, "autoload/EntityRegistry.gd").readText()
        registry `should contain` "const Slime = preload(\"res://entities/enemies/slime.gd\")"
        registry `should contain` "# === EGS-AUTOGEN-BEGIN ==="
    }

    @Test
    fun `writes tres for skill`() {
        val root = seedGameProject()
        scaffolder().scaffold(root, GodotEntityKind.SKILL, "fireball")

        File(root, "entities/skills/fireball.gd").exists() `should be equal to` true
        File(root, "entities/skills/fireball.tres").exists() `should be equal to` true
        File(root, "entities/skills/fireball.tscn").exists() `should be equal to` false
        val registry = File(root, "autoload/EntityRegistry.gd").readText()
        registry `should contain` "const Fireball = preload(\"res://entities/skills/fireball.gd\")"
    }

    @Test
    fun `refuses to overwrite existing file`() {
        val root = seedGameProject()
        // Pre-create the target script.
        File(root, "entities/enemies").mkdirs()
        File(root, "entities/enemies/slime.gd").writeText("hand-written")

        val add = { scaffolder().scaffold(root, GodotEntityKind.ENEMY, "slime") }
        add `should throw` IllegalArgumentException::class
        // Original file untouched.
        File(root, "entities/enemies/slime.gd").readText() `should be equal to` "hand-written"
    }

    @Test
    fun `refuses duplicate registry const`() {
        val root = seedGameProject()
        val scaffolder = scaffolder()
        scaffolder.scaffold(root, GodotEntityKind.ENEMY, "slime")
        // Adding the same name again must fail because the file/const already exist.
        val reAdd = { scaffolder.scaffold(root, GodotEntityKind.ENEMY, "slime") }
        reAdd `should throw` IllegalArgumentException::class
    }

    @Test
    fun `rejects non-godot project`() {
        val root = tmp.toFile()
        File(root, ".egs").mkdirs()
        File(root, ".egs/workspace.json").writeText(
            """
            {
              "name": "android-app",
              "projects": { "game": { "platform": "ANDROID", "path": ".", "basePackage": "com.x" } }
            }
            """.trimIndent(),
        )
        val add = { scaffolder().scaffold(root, GodotEntityKind.ENEMY, "slime") }
        add `should throw` IllegalArgumentException::class
    }

    @Test
    fun `metroidvania overlay fields are emitted`() {
        val root = seedGameProject(gameTemplate = "metroidvania")
        scaffolder().scaffold(root, GodotEntityKind.ENEMY, "patrol_bot")
        val script = File(root, "entities/enemies/patrol_bot.gd").readText()
        script `should contain` "contact_damage = 8.0"
        script `should contain` "drop_table = {}"
    }
}
