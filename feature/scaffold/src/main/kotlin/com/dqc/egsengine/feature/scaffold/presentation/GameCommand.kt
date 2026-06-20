package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotFileAction
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotFileMode
import com.dqc.egsengine.feature.scaffold.data.generator.godot.GodotPlannedFile
import com.dqc.egsengine.feature.scaffold.domain.GodotCodeScaffolder
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.inject

/**
 * `egs game` — entry point for Godot game entity commands.
 *
 * Command tree: `egs game add {enemy,skill,room,item,ui,module} <name>`.
 * Each leaf is contract-driven: the entity kinds are fixed in the CLI, but every
 * output path, template, and registry decision is read from `.egs/generator.json`.
 */
class GameCommand : EgsCliCommand(name = "game", help = "Add entities (enemy/skill/room/item/ui/module) to a Godot game project") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): GameCommand = GameCommand().subcommands(
            GameAddGroupCommand.withSubcommands(),
        )
    }
}

/** `egs game add` — groups the per-kind entity add subcommands. */
class GameAddGroupCommand : EgsCliCommand(name = "add", help = "Add an enemy, skill, room, item, ui, or module to a Godot game") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): GameAddGroupCommand = GameAddGroupCommand().subcommands(
            GameAddEntityCommand("enemy"),
            GameAddEntityCommand("skill"),
            GameAddEntityCommand("room"),
            GameAddEntityCommand("item"),
            GameAddEntityCommand("ui"),
            GameAddEntityCommand("module"),
        )
    }
}

/**
 * One contract-driven add leaf. `egs game add <commandId> <name>` delegates entirely to
 * [GodotCodeScaffolder], which resolves the contract entry for [commandId].
 */
class GameAddEntityCommand(
    private val commandId: String,
) : EgsCliCommand(
    name = commandId,
    help = helpFor(commandId),
) {
    private val scaffolder: GodotCodeScaffolder by inject()

    private val entityName by argument(help = "Entity name (letters, digits, underscore; starts with a letter)")
    private val projectPath by option("--project", "-p", help = "Godot game project root (default: current dir)")
        .default(".")
    private val theme by option("--theme", help = "base | metroidvania (default: from workspace.json)")
    private val force by option("--force", help = "Rebuild Generated files; never overwrites user stubs").flag()
    private val dryRun by option("--dry-run", help = "Preview files and registry change without writing").flag()

    override fun runCommand() {
        val dir = ProjectRootResolver.resolve(projectPath)
        val result = scaffolder.scaffold(dir, commandId, entityName, theme, force, dryRun)

        echo(CliFormatter.formatInfo("${if (dryRun) "[dry-run] " else ""}game add $commandId ${result.preview.pascal}"))
        echo("  module: ${result.preview.module}")
        echo("  theme: ${result.preview.theme}")
        echo()
        echo("Files:")
        result.files.forEach { echo(formatFile(it, dryRun)) }

        if (result.preview.registers) {
            echo()
            echo("Registry:")
            echo("  ${result.preview.registryModuleRelPath}")
            val added = result.registryDiff?.added
            if (added != null) {
                echo("    + $added")
            } else {
                echo("    (already registered — idempotent)")
            }
        }
    }

    private fun formatFile(
        file: GodotPlannedFile,
        dryRun: Boolean,
    ): String = when (file.action) {
        GodotFileAction.CREATE -> {
            val tag = if (dryRun) "would create" else "created"
            "  $tag ${labelFor(file)}: ${file.relPath}"
        }
        GodotFileAction.REBUILD -> "  rebuild ${labelFor(file)}: ${file.relPath}"
        GodotFileAction.SKIP_USER -> "  skip user stub exists: ${file.relPath}"
        GodotFileAction.BLOCKED -> "  blocked (exists, --force to rebuild): ${file.relPath}"
    }

    private fun labelFor(file: GodotPlannedFile): String = when (file.mode) {
        GodotFileMode.USER_CREATE_ONCE -> "user stub"
        GodotFileMode.SCENE_CREATE_ONCE -> "scene"
        GodotFileMode.GENERATED_REBUILDABLE -> "generated"
        GodotFileMode.RESOURCE_REBUILDABLE -> "resource"
        GodotFileMode.STATIC_REBUILDABLE -> "static"
    }
}

private fun helpFor(commandId: String): String = when (commandId) {
    "enemy" -> "Add an enemy: Generated.gd + Stub.gd + scene (registered)"
    "skill" -> "Add a skill: Generated.gd + Stub.gd + .tres (registered)"
    "room" -> "Add a room: Generated.gd + Stub.gd + scene (registered)"
    "item" -> "Add an item data resource (.tres, not registered)"
    "ui" -> "Add a UI controller: Stub.gd + scene (not registered)"
    "module" -> "Scaffold a new gameplay module (dirs + module.json + README)"
    else -> "Add a $commandId entity"
}
