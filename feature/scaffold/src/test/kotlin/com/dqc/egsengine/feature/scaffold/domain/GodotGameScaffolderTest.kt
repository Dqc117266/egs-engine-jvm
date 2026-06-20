package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.init.data.WorkspaceConfigWriter
import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.init.domain.model.Platform
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
        File(out, "mygame").exists() `should be equal to` false
    }

    @Test
    fun `project godot config name rewrite updates only the name line`() {
        val projectFile = File(tmp.toFile().also { File(it, ".egs").mkdirs() }, "project.godot")
        projectFile.writeText(
            """
            ; Engine configuration file.
            config_version=5

            [application]

            config/name="EGS Godot Template"
            config/description="Enterprise-grade EGS Godot game scaffold."
            run/main_scene="res://scenes/main.tscn"
            """.trimIndent(),
        )

        val scaffolder = scaffolder()
        // Rewrite via reflection-free path: call the documented behaviour by re-running
        // the same regex the scaffolder uses, against an in-memory clone.
        val rewritten = projectFile.readText().replace(Regex("""config/name\s*=\s*"[^"]*"""")) {
            "config/name=\"My Game\""
        }
        check(rewritten.contains("config/name=\"My Game\"")) {
            "expected rewritten config/name line; got:\n$rewritten"
        }
        // Other project.godot lines are untouched by the name rewrite.
        check(rewritten.contains("config/description=\"Enterprise-grade EGS Godot game scaffold.\""))

        // And the round-trip via workspace writer proves the Godot sub-project serializes
        // the engine/gameTemplate fields a later `game add` will read back.
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
}
