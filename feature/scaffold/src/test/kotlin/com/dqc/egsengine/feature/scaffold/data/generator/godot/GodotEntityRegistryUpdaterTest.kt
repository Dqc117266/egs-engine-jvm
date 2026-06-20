package com.dqc.egsengine.feature.scaffold.data.generator.godot

import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityKind
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityTemplateModel
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should not contain`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test

class GodotEntityRegistryUpdaterTest {
    private val updater = GodotEntityRegistryUpdater()

    private fun model(
        className: String,
        resPath: String,
    ) = GodotEntityTemplateModel(
        name = className,
        snakeName = className.lowercase(),
        className = className,
        kind = GodotEntityKind.ENEMY,
        gameTemplate = GameTemplate.BASE,
        baseScriptResPath = "res://entities/enemies/core/enemy.gd",
        generatedScriptResPath = resPath,
    )

    private val templateRegistry = """
        extends Node
        ## EntityRegistry

        # === EGS-AUTOGEN-BEGIN ===
        # (egs game add inserts preload() consts here)
        # === EGS-AUTOGEN-END ===


        func _ready() -> void:
            ServiceLocator.register("EntityRegistry", self)
    """.trimIndent()

    @Test
    fun `inserts a const line inside the marked region only`() {
        val update = updater.addEntity(templateRegistry, model("Slime", "res://entities/enemies/slime.gd"))

        update.after `should contain` "const Slime = preload(\"res://entities/enemies/slime.gd\")"
        // Markers and surrounding code are preserved.
        update.after `should contain` GodotEntityRegistryUpdater.BEGIN_MARKER
        update.after `should contain` GodotEntityRegistryUpdater.END_MARKER
        // Each marker appears exactly once (no duplication from the rewrite).
        markerCount(update.after, GodotEntityRegistryUpdater.BEGIN_MARKER) `should be equal to` 1
        markerCount(update.after, GodotEntityRegistryUpdater.END_MARKER) `should be equal to` 1
        update.after `should contain` "func _ready() -> void:"
        update.after `should contain` "ServiceLocator.register(\"EntityRegistry\", self)"
        // Outside-region header text unchanged.
        update.after `should contain` "## EntityRegistry"
    }

    @Test
    fun `appending a second entity keeps the first and stays inside the region`() {
        val first = updater.addEntity(templateRegistry, model("Slime", "res://entities/enemies/slime.gd")).after
        val second = updater.addEntity(first, model("Bat", "res://entities/enemies/bat.gd")).after

        second `should contain` "const Slime = preload(\"res://entities/enemies/slime.gd\")"
        second `should contain` "const Bat = preload(\"res://entities/enemies/bat.gd\")"
        // Markers still appear exactly once after the second rewrite.
        markerCount(second, GodotEntityRegistryUpdater.BEGIN_MARKER) `should be equal to` 1
        markerCount(second, GodotEntityRegistryUpdater.END_MARKER) `should be equal to` 1
        // Header/footer untouched across both writes.
        second `should contain` "ServiceLocator.register(\"EntityRegistry\", self)"
    }

    private fun markerCount(
        text: String,
        marker: String,
    ): Int = text.split(marker).size - 1

    @Test
    fun `rejects duplicate className`() {
        val first = updater.addEntity(templateRegistry, model("Slime", "res://entities/enemies/slime.gd"))
        val reAdd = {
            updater.addEntity(first.after, model("Slime", "res://entities/enemies/slime.gd"))
        }
        reAdd `should throw` IllegalStateException::class
    }

    @Test
    fun `rejects duplicate even when the existing const lives outside the markers`() {
        val withManualConst = templateRegistry.replace(
            "## EntityRegistry",
            "## EntityRegistry\nconst Slime = preload(\"res://entities/enemies/slime.gd\")",
        )
        val reAdd = {
            updater.addEntity(withManualConst, model("Slime", "res://entities/enemies/slime.gd"))
        }
        reAdd `should throw` IllegalStateException::class
    }

    @Test
    fun `errors when markers are missing`() {
        {
            updater.addEntity("extends Node\nfunc _ready(): pass\n", model("Slime", "res://entities/enemies/slime.gd"))
        } `should throw` IllegalArgumentException::class
    }

    @Test
    fun `no const line leaks above the BEGIN marker`() {
        val update = updater.addEntity(templateRegistry, model("Slime", "res://entities/enemies/slime.gd"))
        val aboveBegin = update.after.substringBefore(GodotEntityRegistryUpdater.BEGIN_MARKER)
        aboveBegin `should not contain` "const Slime"
    }

    @Test
    fun `placeholder line remains when region is otherwise empty`() {
        val update = updater.addEntity(templateRegistry, model("Slime", "res://entities/enemies/slime.gd"))
        // After inserting one const, the empty placeholder comment should be gone,
        // and the single const sits between the markers.
        update.after `should not contain` "(egs game add inserts preload() consts here)"
        update.after `should contain` "const Slime = preload(\"res://entities/enemies/slime.gd\")"
        // Exactly one const line in the whole file.
        val constCount = Regex("""const\s+Slime\s+=""").findAll(update.after).count()
        constCount `should be equal to` 1
    }
}
