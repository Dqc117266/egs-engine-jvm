package com.dqc.egsengine.feature.script.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.script.domain.ScriptEngine
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import org.koin.core.component.inject

class ScriptCommand : EgsCliCommand(name = "script") {
    init {
        subcommands(ScriptRun(), ScriptList(), ScriptValidate())
    }

    override fun runCommand() = Unit
}

private class ScriptRun : EgsCliCommand(name = "run") {

    private val scriptEngine: ScriptEngine by inject()
    private val path by argument()

    override fun runCommand() {
        val script =
            scriptEngine.loadScript(path)
                ?: throw IllegalArgumentException("Failed to load script: $path")

        echo(CliFormatter.formatInfo("Running script: ${script.name} (${script.commands.size} commands)"))
        // TODO: Execute script commands through CommandService
        echo(CliFormatter.formatSuccess("Script loaded: ${script.name}"))
    }
}

private class ScriptList : EgsCliCommand(name = "list") {

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

private class ScriptValidate : EgsCliCommand(name = "validate") {

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
