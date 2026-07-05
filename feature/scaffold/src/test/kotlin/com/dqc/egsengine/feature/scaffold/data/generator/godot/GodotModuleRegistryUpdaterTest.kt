package com.dqc.egsengine.feature.scaffold.data.generator.godot

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GodotModuleRegistryUpdaterTest {
    @TempDir
    lateinit var tmp: Path

    private val contract =
        GodotRegistryContract(
            relativePath = "generated/registry.gd",
            beginMarker = "# === EGS-AUTOGEN-BEGIN ===",
            endMarker = "# === EGS-AUTOGEN-END ===",
            lineFormat = """const {className} = preload("res://{scriptPath}")""",
        )
    private val updater = GodotModuleRegistryUpdater(contract)

    @Test
    fun `creates the registry file from template when missing`() {
        val root = tmp.toFile()
        val diff =
            updater.addConst(
                projectRoot = root,
                module = "combat",
                className = "Slime",
                scriptRes = "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd",
            )

        diff.added!! `should contain` "const Slime = preload"
        diff.after `should contain` contract.beginMarker
        diff.after `should contain` contract.endMarker
        diff.after `should contain` "combat generated entity registry"
        diff.after `should contain` "res://modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd"
        // exactly one const, between the markers
        markerCount(diff.after, contract.beginMarker) `should be equal to` 1
        markerCount(diff.after, contract.endMarker) `should be equal to` 1
    }

    @Test
    fun `appends a second const keeping both, sorted`() {
        val root = tmp.toFile()
        // Persist between calls (the scaffolder writes `after` after each addConst).
        persist(root, updater.addConst(root, "combat", "Slime", "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd"))
        val second =
            updater.addConst(root, "combat", "Bat", "modules/combat/generated/entities/enemies/bat/BatGenerated.gd")

        second.after `should contain` "const Slime = preload"
        second.after `should contain` "const Bat = preload"
        // sorted: Bat before Slime
        val batIdx = second.after.indexOf("const Bat")
        val slimeIdx = second.after.indexOf("const Slime")
        (batIdx < slimeIdx) `should be equal to` true
        markerCount(second.after, contract.beginMarker) `should be equal to` 1
    }

    @Test
    fun `duplicate const is idempotent and returns null added`() {
        val root = tmp.toFile()
        persist(root, updater.addConst(root, "combat", "Slime", "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd"))
        val again =
            updater.addConst(root, "combat", "Slime", "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd")

        again.added `should be equal to` null
        updater.hasConst(root, "combat", "Slime") `should be equal to` true
    }

    private fun persist(
        root: File,
        diff: com.dqc.egsengine.feature.scaffold.data.generator.godot.RegistryDiff,
    ) {
        val reg = File(root, "modules/combat/${contract.relativePath}")
        reg.parentFile.mkdirs()
        reg.writeText(diff.after)
    }

    @Test
    fun `preserves content outside the marker region`() {
        val root = tmp.toFile()
        val regFile = File(root, "modules/combat/generated/registry.gd")
        regFile.parentFile.mkdirs()
        regFile.writeText(
            """
            extends Node
            ## hand-written header

            ${contract.beginMarker}
            # (placeholder)
            ${contract.endMarker}

            func get_all() -> Array:
                return []
            """.trimIndent(),
        )

        val diff = updater.addConst(root, "combat", "Slime", "modules/combat/generated/entities/enemies/slime/SlimeGenerated.gd")

        diff.after `should contain` "## hand-written header"
        diff.after `should contain` "func get_all() -> Array:"
        diff.after `should contain` "return []"
        diff.after `should contain` "const Slime = preload"
    }

    private fun markerCount(
        text: String,
        marker: String,
    ): Int = text.split(marker).size - 1
}
