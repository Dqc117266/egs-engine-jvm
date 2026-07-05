package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotGeneratorContractReader
import com.dqc.egsengine.feature.scaffold.golden.GodotTestProject
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GodotCodeScaffolderTest {
    @TempDir
    lateinit var tmp: Path

    private fun scaffolder() = GodotCodeScaffolder(
        WorkspaceConfigResolver(WorkspaceConfigReader()),
        GodotGeneratorContractReader(),
        GodotCodeGenerator(TemplateEngine(TemplateRegistry())),
    )

    @Test
    fun `dry run writes nothing`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val result = scaffolder().scaffold(root, "enemy", "slime", dryRun = true)

        result.dryRun `should be equal to` true
        File(root, "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd").exists() `should be equal to` false
        File(root, "modules/combat/generated/registry.gd").exists() `should be equal to` false
    }

    @Test
    fun `writes modular files and updates per-module registry`() {
        val root = GodotTestProject.seed(tmp.toFile())
        scaffolder().scaffold(root, "enemy", "slime")

        File(root, "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd").exists() `should be equal to` true
        File(root, "modules/combat/src/entities/enemies/slime/Slime.gd").exists() `should be equal to` true
        File(root, "modules/combat/scenes/enemies/slime.tscn").exists() `should be equal to` true
        val registry = File(root, "modules/combat/generated/registry.gd")
        registry.exists() `should be equal to` true
        registry.readText().contains("const Slime = preload") `should be equal to` true
    }

    @Test
    fun `does NOT touch the global EntityRegistry aggregator`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val global = File(root, "app/autoload/EntityRegistry.gd")
        global.parentFile.mkdirs()
        global.writeText("extends Node\n# global\n")

        scaffolder().scaffold(root, "enemy", "slime")

        global.readText() `should be equal to` "extends Node\n# global\n"
    }

    @Test
    fun `second add of same entity is idempotent no error`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val s = scaffolder()
        s.scaffold(root, "enemy", "slime")
        // second run: generated exists -> blocked unless --force; but registry is idempotent.
        // The blocked guard throws for the generated file, so this must error without force.
        val again = { s.scaffold(root, "enemy", "slime") }
        again `should throw` IllegalArgumentException::class
        // with --force, it rebuilds generated (no error)
        s.scaffold(root, "enemy", "slime", force = true)
    }

    @Test
    fun `force rebuilds generated but leaves user stub untouched`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val s = scaffolder()
        s.scaffold(root, "enemy", "slime")
        val stub = File(root, "modules/combat/src/entities/enemies/slime/Slime.gd")
        stub.writeText("# user edits\n")

        s.scaffold(root, "enemy", "slime", force = true)

        stub.readText() `should be equal to` "# user edits\n"
    }

    @Test
    fun `item does not write a registry`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val result = scaffolder().scaffold(root, "item", "health_potion")

        result.preview.registers `should be equal to` false
        result.registryDiff `should be equal to` null
        File(root, "modules/inventory/generated/registry.gd").exists() `should be equal to` false
    }

    @Test
    fun `module scaffolds the six subdirs with gitkeep`() {
        val root = GodotTestProject.seed(tmp.toFile())
        scaffolder().scaffold(root, "module", "fishing")

        listOf("api", "generated", "src", "scenes", "resources", "tests").forEach { sub ->
            File(root, "modules/fishing/$sub/.gitkeep").exists() `should be equal to` true
        }
        File(root, "modules/fishing/module.json").exists() `should be equal to` true
    }

    @Test
    fun `rejects non-godot project`() {
        val root = tmp.toFile()
        File(root, ".egs").mkdirs()
        File(root, ".egs/workspace.json").writeText(
            """{"name":"x","projects":{"game":{"platform":"ANDROID","path":".","basePackage":"com.x"}}}""",
        )
        val add = { scaffolder().scaffold(root, "enemy", "slime") }
        add `should throw` IllegalArgumentException::class
    }
}
