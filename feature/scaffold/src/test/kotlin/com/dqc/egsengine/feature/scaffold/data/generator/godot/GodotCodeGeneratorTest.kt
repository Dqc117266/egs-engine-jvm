package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.scaffold.golden.GodotTestProject
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GodotCodeGeneratorTest {
    @TempDir
    lateinit var tmp: Path

    private val generator = GodotCodeGenerator(TemplateEngine(TemplateRegistry()))

    @Test
    fun `enemy emits generated stub and scene`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, "enemy", "slime", null, force = false)

        val paths = preview.files.map { it.relPath }
        paths `should contain` "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd"
        paths `should contain` "modules/combat/src/entities/enemies/slime/Slime.gd"
        paths `should contain` "modules/combat/scenes/enemies/slime.tscn"
        preview.module `should be equal to` "combat"
        preview.registers `should be equal to` true

        val generated = preview.files.first { it.relPath.endsWith("SlimeGenerated.gd") }.content
        generated `should contain` "extends \"res://modules/combat/api/Enemy.gd\""
        generated `should contain` "class_name SlimeGenerated"
        val scene = preview.files.first { it.relPath.endsWith(".tscn") }.content
        scene `should contain` "res://modules/combat/src/entities/enemies/slime/Slime.gd"
    }

    @Test
    fun `skill emits generated stub and tres`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, "skill", "fireball", null, force = false)

        val paths = preview.files.map { it.relPath }
        paths `should contain` "modules/skills/generated/entities/skills/fireball/FireballGenerated.gd"
        paths `should contain` "modules/skills/src/entities/skills/fireball/Fireball.gd"
        paths `should contain` "modules/skills/generated/resources/skills/fireball.tres"
        preview.module `should be equal to` "skills"
    }

    @Test
    fun `item emits only tres and does not register`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, "item", "health_potion", null, force = false)

        preview.files.map { it.relPath } `should contain` "modules/inventory/generated/resources/items/health_potion.tres"
        preview.files.size `should be equal to` 1
        preview.registers `should be equal to` false
    }

    @Test
    fun `ui emits stub and scene without generated layer`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, "ui", "inventory_panel", null, force = false)

        val paths = preview.files.map { it.relPath }
        paths `should contain` "modules/ui/src/InventoryPanel.gd"
        paths `should contain` "modules/ui/scenes/inventory_panel.tscn"
        preview.files.none { it.relPath.contains("Generated") } `should be equal to` true
    }

    @Test
    fun `metroidvania theme overrides the generated template`() {
        val root = GodotTestProject.seed(tmp.toFile(), theme = "metroidvania")
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, "enemy", "bat", null, force = false)

        val generated = preview.files.first { it.relPath.endsWith("BatGenerated.gd") }.content
        generated `should contain` "max_hp = 20.0"
        generated `should contain` "hp = max_hp"
        preview.theme `should be equal to` "metroidvania"
    }

    @Test
    fun `module emits module json and readme`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, "module", "fishing", null, force = false)

        val paths = preview.files.map { it.relPath }
        paths `should contain` "modules/fishing/module.json"
        paths `should contain` "modules/fishing/README.md"
        preview.module `should be equal to` "fishing"
        val json = preview.files.first { it.relPath.endsWith("module.json") }.content
        json `should contain` "\"id\": \"fishing\""
    }

    @Test
    fun `existing user stub is skipped, existing generated without force is blocked`() {
        val root = GodotTestProject.seed(tmp.toFile())
        val contract = GodotTestProject.readContract(root)
        // pre-create the user stub and the generated file
        val stub = java.io.File(root, "modules/combat/src/entities/enemies/slime/Slime.gd")
        stub.parentFile.mkdirs()
        stub.writeText("hand-written")
        val gen = java.io.File(root, "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd")
        gen.parentFile.mkdirs()
        gen.writeText("old generated")

        val preview = generator.preview(root, contract, "enemy", "slime", null, force = false)
        val stubFile = preview.files.first { it.relPath.endsWith("Slime.gd") }
        val genFile = preview.files.first { it.relPath.endsWith("SlimeGenerated.gd") }
        stubFile.action `should be equal to` GodotFileAction.SKIP_USER
        genFile.action `should be equal to` GodotFileAction.BLOCKED

        // with force, generated becomes REBUILD; user stub stays SKIP_USER
        val forced = generator.preview(root, contract, "enemy", "slime", null, force = true)
        forced.files.first { it.relPath.endsWith("SlimeGenerated.gd") }.action `should be equal to` GodotFileAction.REBUILD
        forced.files.first { it.relPath.endsWith("Slime.gd") }.action `should be equal to` GodotFileAction.SKIP_USER
    }
}
