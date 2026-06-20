package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.init.domain.model.GameTemplate
import com.dqc.egsengine.feature.scaffold.domain.GodotGameScaffolder
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import org.koin.core.component.inject
import java.io.File

/**
 * `egs new game <name> --engine godot --template base|metroidvania`.
 *
 * Scaffolds a standalone Godot game project from the egs-godot-template, recording
 * the chosen template flavour into `.egs/workspace.json` so later `egs game add`
 * commands pick up the right field overlay.
 */
class NewGameCommand : EgsCliCommand(name = "game", help = "Create a new Godot game project (--engine godot)") {
    private val scaffolder: GodotGameScaffolder by inject()

    private val gameName by argument(help = "Game name (also the output directory name)")

    private val engine by option(
        "--engine",
        help = "Game engine. Currently only 'godot' is supported.",
    ).default("godot")

    private val gameTemplate by option(
        "--template",
        help = "Game template flavour: base (default) or metroidvania",
    ).enum<GameTemplate>(ignoreCase = true).default(GameTemplate.BASE)

    private val outputPath by option("--output", "-o", help = "Output directory (default: current dir)")
        .default(".")

    private val gitProtocol by option(
        "--protocol",
        help = "Clone URL scheme: ssh (default) or https. Env: EGS_ENGINE_GIT_PROTOCOL",
    )
    private val githubToken by option("--token", help = "GitHub PAT for HTTPS clone of private repos")
    private val githubUsername by option("--username", help = "GitHub username for token URL")
        .default("x-access-token")
    private val templateUrl by option(
        "--game-template",
        help = "Override the Godot base-template git URL (advanced)",
    )

    private val dryRun by option("--dry-run", help = "Preview clone + workspace config without writing").flag()

    override fun runCommand() {
        require(engine.trim().equals("godot", ignoreCase = true)) {
            "Unsupported --engine '$engine'. Only 'godot' is supported by 'egs new game'."
        }
        NewProjectInputResolver.validateProjectName(gameName)

        val outputDir = File(outputPath).absoluteFile

        val result =
            scaffolder.scaffold(
                outputDir = outputDir,
                gameName = gameName,
                gameTemplate = gameTemplate,
                gitProtocolCli = gitProtocol,
                githubToken = githubToken,
                githubUsername = githubUsername,
                templateUrlOverride = templateUrl,
                dryRun = dryRun,
            ) { echo(CliFormatter.formatInfo(it)) }

        echo()
        if (dryRun) {
            echo(CliFormatter.formatInfo("[dry-run] Would create Godot game at ${result.targetDir.absolutePath}"))
            echo("  Template URL: ${result.templateUrl}")
        } else {
            echo(CliFormatter.formatSuccess("Godot game created"))
            echo("  Path: ${result.targetDir.absolutePath}")
        }
        echo("  Name: ${result.gameName}")
        echo("  Engine: godot")
        echo("  Template: ${result.gameTemplate.id}")
        if (!dryRun) {
            echo("  Config: ${result.targetDir.resolve(".egs/workspace.json").absolutePath}")
        }
    }
}
