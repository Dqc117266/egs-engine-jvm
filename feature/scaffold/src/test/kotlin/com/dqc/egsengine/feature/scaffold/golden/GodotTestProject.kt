package com.dqc.egsengine.feature.scaffold.golden

import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotGeneratorContract
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotGeneratorContractReader
import java.io.File

/**
 * Seeds a minimal Godot project for tests: `.egs/generator.json` + `.egs/workspace.json`.
 * Templates resolve from the engine's bundled classpath fallback (no project `.egs/templates/`
 * needed), so this fixture stays small. Mirrors the real egs-godot-template contract.
 */
object GodotTestProject {
    /** The full 6-command contract (enemy/skill/room/item/ui/module), matching generator.json v2. */
    private val CONTRACT_JSON = """
        {
          "version": "2",
          "description": "test contract",
          "naming": { "inputPattern": "^[A-Za-z][A-Za-z0-9_]*$", "fileNameCase": "snake_case", "classNameCase": "PascalCase" },
          "registry": { "mode": "per-module", "relativePath": "generated/registry.gd", "globalAggregator": "app/autoload/EntityRegistry.gd", "beginMarker": "# === EGS-AUTOGEN-BEGIN ===", "endMarker": "# === EGS-AUTOGEN-END ===", "lineFormat": "const {className} = preload(\"res://{scriptPath}\")" },
          "templates": { "root": ".egs/templates/godot", "defaultTheme": "base", "themes": ["base", "metroidvania"] },
          "commands": {
            "enemy": {
              "module": "combat",
              "summary": "enemy",
              "generatedScriptPath": "modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd",
              "userScriptPath": "modules/{module}/src/entities/enemies/{snake}/{pascal}.gd",
              "scenePath": "modules/{module}/scenes/enemies/{snake}.tscn",
              "generatedExtendsPath": "res://modules/{module}/api/Enemy.gd",
              "userExtendsPath": "res://modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd",
              "templateIds": ["generated.gd.ftl", "stub.gd.ftl", "scene.tscn.ftl"],
              "registers": true,
              "registryScriptPath": "modules/{module}/generated/entities/enemies/{snake}/{pascal}Generated.gd"
            },
            "skill": {
              "module": "skills",
              "summary": "skill",
              "generatedScriptPath": "modules/{module}/generated/entities/skills/{snake}/{pascal}Generated.gd",
              "userScriptPath": "modules/{module}/src/entities/skills/{snake}/{pascal}.gd",
              "dataPath": "modules/{module}/generated/resources/skills/{snake}.tres",
              "generatedExtendsPath": "res://modules/{module}/api/Skill.gd",
              "userExtendsPath": "res://modules/{module}/generated/entities/skills/{snake}/{pascal}Generated.gd",
              "templateIds": ["generated.gd.ftl", "stub.gd.ftl", "data.tres.ftl"],
              "registers": true,
              "registryScriptPath": "modules/{module}/generated/entities/skills/{snake}/{pascal}Generated.gd"
            },
            "room": {
              "module": "world",
              "summary": "room",
              "generatedScriptPath": "modules/{module}/generated/entities/rooms/{snake}/{pascal}Generated.gd",
              "userScriptPath": "modules/{module}/src/entities/rooms/{snake}/{pascal}.gd",
              "scenePath": "modules/{module}/scenes/rooms/{snake}.tscn",
              "generatedExtendsPath": "res://modules/{module}/api/Room.gd",
              "userExtendsPath": "res://modules/{module}/generated/entities/rooms/{snake}/{pascal}Generated.gd",
              "templateIds": ["generated.gd.ftl", "stub.gd.ftl", "scene.tscn.ftl"],
              "registers": true,
              "registryScriptPath": "modules/{module}/generated/entities/rooms/{snake}/{pascal}Generated.gd"
            },
            "item": {
              "module": "inventory",
              "summary": "item",
              "dataPath": "modules/{module}/generated/resources/items/{snake}.tres",
              "generatedExtendsPath": null,
              "templateIds": ["data.tres.ftl"],
              "registers": false
            },
            "ui": {
              "module": "ui",
              "summary": "ui",
              "userScriptPath": "modules/{module}/src/{pascal}.gd",
              "scenePath": "modules/{module}/scenes/{snake}.tscn",
              "userExtendsPath": "Control",
              "templateIds": ["stub.gd.ftl", "scene.tscn.ftl"],
              "registers": false
            },
            "module": {
              "module": "{snake}",
              "summary": "module",
              "moduleJsonPath": "modules/{snake}/module.json",
              "readmePath": "modules/{snake}/README.md",
              "templateIds": ["module.json.ftl", "readme.md.ftl"],
              "registers": false
            }
          }
        }
    """.trimIndent()

    private val WORKSPACE_JSON = """
        {
          "name": "test-game",
          "version": "3",
          "projects": {
            "game": { "platform": "GODOT", "path": ".", "basePackage": "", "engine": "godot", "gameTemplate": "base" }
          }
        }
    """.trimIndent()

    /** Seed a minimal godot project; returns the project root. [theme] overrides gameTemplate in workspace. */
    fun seed(
        root: File,
        theme: String = "base",
    ): File {
        File(root, ".egs").mkdirs()
        File(root, ".egs/generator.json").writeText(CONTRACT_JSON)
        File(root, ".egs/workspace.json").writeText(WORKSPACE_JSON.replace("\"gameTemplate\": \"base\"", "\"gameTemplate\": \"$theme\""))
        return root
    }

    fun readContract(root: File): GodotGeneratorContract = GodotGeneratorContractReader().read(root)
}
