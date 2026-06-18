package com.dqc.egsengine.feature.command.presentation

import com.dqc.egsengine.feature.base.presentation.CliError
import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.command.domain.CommandService
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

class ShellCommand : EgsCliCommand(name = "command", help = "Execute shell commands with timeout and streaming output") {

    private val commandService: CommandService by inject()
    private val workDir by option("--dir", "-d")

    /** --raw: suppress success banner, only output child process stdout (for scripting). */
    private val raw by option("--raw").flag()

    /** --timeout: kill process after N seconds (exit code -1 on timeout). */
    private val timeoutSec by option("--timeout", help = "Kill process after N seconds").int()

    private val shellArgs by argument().multiple(required = true)

    override fun runCommand() = runBlocking {
        val shellCommand = shellArgs.joinToString(" ")
        val timeoutMs = timeoutSec?.toLong()?.times(1000)
        val result = commandService.executeCommand(shellCommand, workDir, timeoutMs)

        if (result.isSuccess) {
            if (!raw) {
                echo(CliFormatter.formatSuccess("Command executed successfully"))
            }
            if (result.output.isNotBlank()) {
                echo(result.output)
            }
        } else {
            // P1：透传子进程失败 —— 非 0 退出，stderr 给出 exit code。
            if (result.error.isNotBlank()) {
                echo(result.error, err = true)
            }
            throw CliError.GenericError("Command failed with exit code: ${result.exitCode}")
        }
    }
}
