package com.dqc.egsengine.feature.scaffold.data.generator.godot

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GodotGeneratorContractReaderTest {
    @TempDir
    lateinit var tmp: Path

    private val reader = GodotGeneratorContractReader()

    @Test
    fun `reads generator json into the contract model`() {
        val root = seedContract()
        val contract = reader.read(root)

        contract.version `should be equal to` "2"
        contract.naming.inputPattern `should be equal to` "^[A-Za-z][A-Za-z0-9_]*$"
        contract.registry.mode `should be equal to` "per-module"
        contract.registry.relativePath `should be equal to` "generated/registry.gd"
        contract.templates.themes `should be equal to` listOf("base", "metroidvania")
        contract.commands.keys `should contain` "enemy"
        val enemy = contract.commands.getValue("enemy")
        enemy.module `should be equal to` "combat"
        enemy.registers `should be equal to` true
        enemy.templateIds `should be equal to` listOf("generated.gd.ftl", "stub.gd.ftl", "scene.tscn.ftl")
    }

    @Test
    fun `requireCommand lists valid commands when unknown`() {
        val root = seedContract()
        val err = assertThrows<IllegalStateException> { reader.requireCommand(root, "dragon") }.message!!
        err `should contain` "Unknown entity type 'dragon'"
        err `should contain` "enemy"
    }

    @Test
    fun `errors clearly when generator json is missing`() {
        val root = tmp.toFile()
        val err = assertThrows<IllegalArgumentException> { reader.read(root) }.message!!
        err `should contain` "Not an egs-godot project root"
        err `should contain` "generator.json"
    }

    /** Seed a minimal generator.json covering enemy + item (registers vs not). */
    private fun seedContract() = tmp.toFile().also { root ->
        java.io.File(root, ".egs").mkdirs()
        java.io.File(root, ".egs/generator.json").writeText(
            """
                {
                  "version": "2",
                  "naming": { "inputPattern": "^[A-Za-z][A-Za-z0-9_]*$", "fileNameCase": "snake_case", "classNameCase": "PascalCase" },
                  "registry": { "mode": "per-module", "relativePath": "generated/registry.gd", "globalAggregator": "app/autoload/EntityRegistry.gd", "beginMarker": "# === EGS-AUTOGEN-BEGIN ===", "endMarker": "# === EGS-AUTOGEN-END ===", "lineFormat": "const {className} = preload(\"res://{scriptPath}\")" },
                  "templates": { "root": ".egs/templates/godot", "defaultTheme": "base", "themes": ["base", "metroidvania"] },
                  "commands": {
                    "enemy": {
                      "module": "combat",
                      "generatedScriptPath": "modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd",
                      "userScriptPath": "modules/{module}/src/entities/enemies/{snake}/{pascal}.gd",
                      "scenePath": "modules/{module}/scenes/enemies/{snake}.tscn",
                      "generatedExtendsPath": "res://modules/{module}/api/Enemy.gd",
                      "userExtendsPath": "res://modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd",
                      "templateIds": ["generated.gd.ftl", "stub.gd.ftl", "scene.tscn.ftl"],
                      "registers": true,
                      "registryScriptPath": "modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd"
                    },
                    "item": {
                      "module": "inventory",
                      "dataPath": "modules/{module}/generated/resources/items/{snake}.tres",
                      "generatedExtendsPath": null,
                      "templateIds": ["data.tres.ftl"],
                      "registers": false
                    },
                    "module": {
                      "module": "{snake}",
                      "moduleJsonPath": "modules/{snake}/module.json",
                      "readmePath": "modules/{snake}/README.md",
                      "templateIds": ["module.json.ftl", "readme.md.ftl"],
                      "registers": false
                    }
                  }
                }
            """.trimIndent(),
        )
    }
}
