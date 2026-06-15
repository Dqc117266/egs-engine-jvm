package com.dqc.egsengine.feature.command.presentation

import com.dqc.egsengine.feature.base.presentation.CliError
import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.command.domain.CommandService
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

class ShellCommand : EgsCliCommand(name = "command") {

    private val commandService: CommandService by inject()
    private val workDir by option("--dir", "-d")

    /** --raw：不打印成功 banner，仅输出子进程 stdout（便于脚本管道）。 */
    private val raw by option("--raw").flag()
    private val shellArgs by argument().multiple(required = true)

    override fun runCommand() = runBlocking {
        val shellCommand = shellArgs.joinToString(" ")
        val result = commandService.executeCommand(shellCommand, workDir)

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
