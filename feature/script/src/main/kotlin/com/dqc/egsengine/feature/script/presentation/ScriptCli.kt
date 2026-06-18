package com.dqc.egsengine.feature.script.presentation

import com.dqc.egsengine.feature.base.command.CommandExecutor
import com.dqc.egsengine.feature.base.presentation.CliError
import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.script.domain.ScriptEngine
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.io.File

class ScriptCommand : EgsCliCommand(name = "script", help = "Load and run .egs scripts") {
    init {
        subcommands(ScriptRun(), ScriptList(), ScriptValidate())
    }

    override fun runCommand() = Unit
}

private class ScriptRun : EgsCliCommand(name = "run", help = "Run an .egs script file") {

    private val scriptEngine: ScriptEngine by inject()
    private val commandExecutor: CommandExecutor by inject()
    private val path by argument()
    private val workDir by option("--dir", "-d")

    override fun runCommand() {
        val script =
            scriptEngine.loadScript(path)
                ?: throw IllegalArgumentException("Failed to load script: $path")

        val dir = File(workDir ?: System.getProperty("user.dir"))
        echo(CliFormatter.formatInfo("Running script: ${script.name} (${script.commands.size} commands) in ${dir.absolutePath}"))

        val shell = if (System.getProperty("os.name").startsWith("Windows")) "cmd" else "sh"
        val shellFlag = if (shell == "cmd") "/c" else "-c"

        script.commands.forEachIndexed { index, cmd ->
            echo("[${index + 1}/${script.commands.size}] $cmd")
            val result = runBlocking { commandExecutor.execute(listOf(shell, shellFlag, cmd), dir) }
            if (result.output.isNotBlank()) echo(result.output)
            if (!result.isSuccess) {
                if (result.error.isNotBlank()) echo(result.error, err = true)
                throw CliError.GenericError(
                    "Script '${script.name}' failed at command ${index + 1} ($cmd) with exit code ${result.exitCode}",
                )
            }
        }
        echo(CliFormatter.formatSuccess("Script completed: ${script.name} (${script.commands.size} commands)"))
    }
}

private class ScriptList : EgsCliCommand(name = "list", help = "List available .egs scripts") {

    private val scriptEngine: ScriptEngine by inject()
    private val directory by argument().default(".")

    override fun runCommand() {
        val scripts = scriptEngine.listScripts(directory)

        if (scripts.isEmpty()) {
            echo(CliFormatter.formatInfo("No scripts found in: $directory"))
            return
        }

        val table = CliFormatter.formatTable(
            headers = listOf("Name", "Commands", "Source"),
            rows = scripts.map { listOf(it.name, it.commands.size.toString(), it.sourcePath) },
        )
        echo(table)
    }
}

private class ScriptValidate : EgsCliCommand(name = "validate", help = "Validate an .egs script without executing") {

    private val scriptEngine: ScriptEngine by inject()
    private val path by argument()

    override fun runCommand() {
        val script =
            scriptEngine.loadScript(path)
                ?: throw IllegalArgumentException("Failed to load script: $path")

        val errors = scriptEngine.validateScript(script)

        if (errors.isEmpty()) {
            echo(CliFormatter.formatSuccess("Script '${script.name}' is valid"))
        } else {
            errors.forEach { echo(CliFormatter.formatError(it)) }
            throw IllegalArgumentException("Script validation failed with ${errors.size} error(s)")
        }
    }
}
