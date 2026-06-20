package com.dqc.egsengine.feature.scaffold.golden

import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotEntityGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityKind
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.junit.jupiter.api.Test

/**
 * Golden snapshots of `egs game add` entity output for each kind + the metroidvania
 * field overlay. Catches any unintended drift in the Godot entity FTL templates.
 *
 * Refresh with: ./gradlew :feature:scaffold:test -Degs.golden.update=true
 */
class GoldenGodotEntityTest {
    private val engine = GodotEntityGenerator(TemplateEngine(TemplateRegistry()))

    @Test
    fun `enemy base golden`() {
        val preview = engine.preview("slime", GodotEntityKind.ENEMY, GameTemplate.BASE)
        GoldenSnapshot.verify("game-add-enemy-base", preview.files.map { it.path to it.content })
    }

    @Test
    fun `skill base golden`() {
        val preview = engine.preview("fireball", GodotEntityKind.SKILL, GameTemplate.BASE)
        GoldenSnapshot.verify("game-add-skill-base", preview.files.map { it.path to it.content })
    }

    @Test
    fun `room base golden`() {
        val preview = engine.preview("boss_arena", GodotEntityKind.ROOM, GameTemplate.BASE)
        GoldenSnapshot.verify("game-add-room-base", preview.files.map { it.path to it.content })
    }

    @Test
    fun `enemy metroidvania golden`() {
        val preview = engine.preview("patrol_bot", GodotEntityKind.ENEMY, GameTemplate.METROIDVANIA)
        GoldenSnapshot.verify("game-add-enemy-metroidvania", preview.files.map { it.path to it.content })
    }

    @Test
    fun `skill metroidvania golden`() {
        val preview = engine.preview("double_jump", GodotEntityKind.SKILL, GameTemplate.METROIDVANIA)
        GoldenSnapshot.verify("game-add-skill-metroidvania", preview.files.map { it.path to it.content })
    }
}
