package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.init.data.WorkspaceConfigWriter
import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.init.domain.model.Platform
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should not contain`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GodotGameScaffolderTest {
    @TempDir
    lateinit var tmp: Path

    private fun scaffolder() = GodotGameScaffolder(WorkspaceConfigWriter())

    @Test
    fun `dry run produces preview without writing any files`() {
        val out = tmp.toFile()
        val result = scaffolder().scaffold(
            outputDir = out,
            gameName = "mygame",
            gameTemplate = GameTemplate.METROIDVANIA,
            dryRun = true,
        )

        result.dryRun `should be equal to` true
        result.gameName `should be equal to` "mygame"
        result.gameTemplate `should be equal to` GameTemplate.METROIDVANIA
        // Workspace fields captured for later `game add`.
        val game = result.workspace.projects.getValue("game")
        game.platform `should be equal to` Platform.GODOT
        game.engine `should be equal to` "godot"
        game.gameTemplate `should be equal to` "metroidvania"
        // Nothing on disk in dry-run mode.
        check(!java.io.File(out, "mygame").exists())
    }

    @Test
    fun `project godot config name rewrite updates only the name line`() {
        val original =
            """
            ; Engine configuration file.
            config_version=5

            [application]

            config/name="EGS Godot Template"
            config/description="Enterprise-grade EGS Godot game scaffold."
            run/main_scene="res://scenes/main.tscn"
            """.trimIndent()

        // Drive the real code path used by GodotGameScaffolder.rewriteGodotProjectName().
        val rewritten = GodotGameScaffolder.rewriteConfigName(original, "My Game")

        rewritten `should contain` "config/name=\"My Game\""
        // Only the name line changes; every other line is returned verbatim.
        rewritten `should contain` "config/description=\"Enterprise-grade EGS Godot game scaffold.\""
        rewritten `should contain` "run/main_scene=\"res://scenes/main.tscn\""
        rewritten `should not contain` "EGS Godot Template"

        // The workspace round-trip proves the Godot sub-project serializes the
        // engine/gameTemplate fields a later `game add` will read back.
        val writer = WorkspaceConfigWriter()
        writer.write(
            com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig(
                name = "My Game",
                version = "2",
                projects = mapOf(
                    "game" to com.dqc.egsengine.feature.init.domain.model.SubProjectConfig(
                        platform = Platform.GODOT,
                        path = ".",
                        basePackage = "",
                        engine = "godot",
                        gameTemplate = "base",
                    ),
                ),
            ),
            tmp.toFile(),
        )
        val readBack = WorkspaceConfigReader().read(tmp.toFile()).projects.getValue("game")
        readBack.platform `should be equal to` Platform.GODOT
        readBack.engine `should be equal to` "godot"
        readBack.gameTemplate `should be equal to` "base"
    }

    /**
     * Regression: the cloned egs-godot-template ships a rich workspace.json whose
     * `moduleStructure` (no `hasRes`, has `moduleDirs`) and `baseClasses` (Godot
     * className/scriptPath/type, not Android name/packageName/module/filePath)
     * must round-trip through the engine without deserialization errors.
     */
    @Test
    fun `tolerates the cloned godot template rich workspace shape`() {
        val root = tmp.toFile()
        java.io.File(root, ".egs").mkdirs()
        java.io.File(root, ".egs/workspace.json").writeText(
            """
            {
              "name": "egs-godot-template",
              "version": "3",
              "projects": {
                "game": {
                  "platform": "GODOT",
                  "path": ".",
                  "basePackage": "",
                  "engine": "godot",
                  "gameTemplate": "base",
                  "moduleStructure": {
                    "layers": ["app", "foundation", "modules"],
                    "moduleDirs": ["api", "generated", "src", "scenes", "resources", "tests"]
                  },
                  "baseClasses": [
                    { "className": "Enemy", "scriptPath": "res://modules/combat/api/Enemy.gd", "type": "CharacterBody2D", "kind": "OPEN_CLASS" }
                  ]
                }
              }
            }
            """.trimIndent(),
        )

        val game = WorkspaceConfigReader().read(root).projects.getValue("game")
        game.platform `should be equal to` Platform.GODOT
        game.moduleStructure?.moduleDirs `should be equal to`
            listOf("api", "generated", "src", "scenes", "resources", "tests")
        game.baseClasses.size `should be equal to` 1
        game.baseClasses.first().godotClassName `should be equal to` "Enemy"
        game.baseClasses.first().godotScriptPath `should be equal to` "res://modules/combat/api/Enemy.gd"
        game.baseClasses.first().kind `should be equal to`
            com.dqc.egsengine.feature.init.domain.model.BaseClassKind.OPEN_CLASS
    }
}
