package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

class LintCommand : EgsCliCommand(name = "lint") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): LintCommand = LintCommand().subcommands(
            LintFixCommand(),
        )
    }
}

class LintFixCommand : EgsCliCommand(name = "fix") {
    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    override fun runCommand() {
        echo(CliFormatter.formatWarning("lint fix is not yet implemented. Use detektCheck/spotlessCheck via Gradle directly."))
    }
}
