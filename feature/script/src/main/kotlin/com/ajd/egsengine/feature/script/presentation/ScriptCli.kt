package com.ajd.egsengine.feature.script.presentation

import com.ajd.egsengine.feature.base.presentation.CliFormatter
import com.ajd.egsengine.feature.script.domain.ScriptEngine
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScriptCommand : CliktCommand(name = "script") {
    init {
        subcommands(ScriptRun(), ScriptList(), ScriptValidate())
    }

    override fun run() = Unit
}

private class ScriptRun : CliktCommand(name = "run"), KoinComponent {

    private val scriptEngine: ScriptEngine by inject()
    private val path by argument()

    override fun run() {
        val script = scriptEngine.loadScript(path)
        if (script == null) {
            echo(CliFormatter.formatError("Failed to load script: $path"), err = true)
            return
        }

        echo(CliFormatter.formatInfo("Running script: ${script.name} (${script.commands.size} commands)"))
        // TODO: Execute script commands through CommandService
        echo(CliFormatter.formatSuccess("Script loaded: ${script.name}"))
    }
}

private class ScriptList : CliktCommand(name = "list"), KoinComponent {

    private val scriptEngine: ScriptEngine by inject()
    private val directory by argument().default(".")

    override fun run() {
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

private class ScriptValidate : CliktCommand(name = "validate"), KoinComponent {

    private val scriptEngine: ScriptEngine by inject()
    private val path by argument()

    override fun run() {
        val script = scriptEngine.loadScript(path)
        if (script == null) {
            echo(CliFormatter.formatError("Failed to load script: $path"), err = true)
            return
        }

        val errors = scriptEngine.validateScript(script)

        if (errors.isEmpty()) {
            echo(CliFormatter.formatSuccess("Script '${script.name}' is valid"))
        } else {
            errors.forEach { echo(CliFormatter.formatError(it)) }
            echo(CliFormatter.formatError("Script validation failed with ${errors.size} error(s)"))
        }
    }
}
