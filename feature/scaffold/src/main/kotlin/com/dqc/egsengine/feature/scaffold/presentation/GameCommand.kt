package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.GodotEntityScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.GodotEntityKind
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.inject

class GameCommand : EgsCliCommand(name = "game", help = "Add entities (enemy/skill/room) to a Godot game project") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): GameCommand = GameCommand().subcommands(
            GameAddEnemyCommand(),
            GameAddSkillCommand(),
            GameAddRoomCommand(),
        )
    }
}

/** Shared logic for `egs game add <kind> <name>`. */
abstract class GameAddCommand(
    name: String,
    help: String,
    private val kind: GodotEntityKind,
) : EgsCliCommand(name = name, help = help) {
    private val scaffolder: GodotEntityScaffolder by inject()

    protected val entityName by argument(help = "Entity name (letters, digits, underscore; starts with a letter)")
    private val projectPath by option("--project", "-p", help = "Godot game project root (default: current dir)")
        .default(".")
    private val dryRun by option("--dry-run", help = "Preview files and registry change without writing").flag()

    override fun runCommand() {
        val dir = ProjectRootResolver.resolve(projectPath)
        val result = scaffolder.scaffold(dir, kind, entityName, dryRun = dryRun)

        if (result.dryRun) {
            echo(CliFormatter.formatInfo("[dry-run] Would generate ${kind.id} '${result.className}' for game '${dir.name}' (template=${result.gameTemplate.id})"))
            echo()
            echo("  Files:")
            result.files.forEach { echo("    ${it.path}") }
            echo()
            echo("  Registry (${result.registryFile}):")
            echo("    + ${result.className} (preload added inside EGS-AUTOGEN region)")
        } else {
            echo(CliFormatter.formatSuccess("Generated ${kind.id} '${result.className}' (template=${result.gameTemplate.id})"))
            echo()
            echo("  Files created:")
            result.files.forEach { echo("    ${it.path}") }
            echo()
            echo("  Registered in ${result.registryFile}: EntityRegistry.${result.className}")
        }
    }
}

class GameAddEnemyCommand : GameAddCommand(name = "enemy", help = "Add an enemy entity (.gd + .tscn)", kind = GodotEntityKind.ENEMY)

class GameAddSkillCommand : GameAddCommand(name = "skill", help = "Add a skill entity (.gd + .tres)", kind = GodotEntityKind.SKILL)

class GameAddRoomCommand : GameAddCommand(name = "room", help = "Add a room entity (.gd + .tscn)", kind = GodotEntityKind.ROOM)
