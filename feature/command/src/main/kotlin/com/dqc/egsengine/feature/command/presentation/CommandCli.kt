package com.dqc.egsengine.feature.command.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.command.domain.CommandService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ShellCommand : CliktCommand(name = "command"), KoinComponent {

    private val commandService: CommandService by inject()
    private val workDir by option("--dir", "-d")
    private val shellArgs by argument().multiple(required = true)

    override fun run() = runBlocking {
        val shellCommand = shellArgs.joinToString(" ")
        val result = commandService.executeCommand(shellCommand, workDir)

        if (result.isSuccess) {
            echo(CliFormatter.formatSuccess("Command executed successfully"))
            if (result.output.isNotBlank()) {
                echo(result.output)
            }
        } else {
            echo(CliFormatter.formatError("Command failed with exit code: ${result.exitCode}"), err = true)
            if (result.error.isNotBlank()) {
                echo(result.error, err = true)
            }
        }
    }
}
