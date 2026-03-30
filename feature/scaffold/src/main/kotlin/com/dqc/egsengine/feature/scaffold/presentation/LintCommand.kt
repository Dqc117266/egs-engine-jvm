package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

class LintCommand : CliktCommand(name = "lint") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): LintCommand =
            LintCommand().subcommands(
                LintFixCommand(),
            )
    }
}

class LintFixCommand : CliktCommand(name = "fix") {

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    override fun run() {
        echo(CliFormatter.formatInfo("egs lint fix -- not yet implemented"))
    }
}
