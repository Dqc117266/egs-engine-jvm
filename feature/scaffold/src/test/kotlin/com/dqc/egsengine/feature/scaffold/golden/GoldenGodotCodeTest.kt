package com.dqc.egsengine.feature.scaffold.golden

import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotCodeGenerator
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Golden snapshots of `egs game add` output for each command + theme overlay, resolved against
 * the engine's bundled fallback templates (no project `.egs/templates/` seeded).
 *
 * Refresh with: ./gradlew :feature:scaffold:test -Degs.golden.update=true
 *   -Dtest.single=GoldenGodotCodeTest
 */
class GoldenGodotCodeTest {
    @TempDir
    lateinit var tmp: Path

    private val generator = GodotCodeGenerator(TemplateEngine(TemplateRegistry()))

    @Test
    fun `enemy base golden`() {
        golden("godot/enemy-base", "enemy", "slime", "base")
    }

    @Test
    fun `enemy metroidvania golden`() {
        golden("godot/enemy-metroidvania", "enemy", "bat", "metroidvania")
    }

    @Test
    fun `skill base golden`() {
        golden("godot/skill-base", "skill", "fireball", "base")
    }

    @Test
    fun `skill metroidvania golden`() {
        golden("godot/skill-metroidvania", "skill", "double_jump", "metroidvania")
    }

    @Test
    fun `room base golden`() {
        golden("godot/room-base", "room", "boss_arena", "base")
    }

    @Test
    fun `item base golden`() {
        golden("godot/item-base", "item", "health_potion", "base")
    }

    @Test
    fun `ui base golden`() {
        golden("godot/ui-base", "ui", "inventory_panel", "base")
    }

    @Test
    fun `module base golden`() {
        golden("godot/module-base", "module", "fishing", "base")
    }

    private fun golden(
        caseName: String,
        command: String,
        name: String,
        theme: String,
    ) {
        val root = GodotTestProject.seed(tmp.toFile(), theme = theme)
        val contract = GodotTestProject.readContract(root)
        val preview = generator.preview(root, contract, command, name, theme, force = false)
        val files = preview.files.filter { it.content.isNotEmpty() }.map { it.relPath to it.content }
        GoldenSnapshot.verify(caseName, files)
    }
}
