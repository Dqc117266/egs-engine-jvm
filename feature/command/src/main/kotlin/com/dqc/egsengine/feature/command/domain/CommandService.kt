package com.dqc.egsengine.feature.command.domain

import com.dqc.egsengine.feature.base.command.CommandResult
import com.dqc.egsengine.feature.command.data.ShellCommandRunner
import org.slf4j.LoggerFactory
import java.io.File

class CommandService(
    private val shellCommandRunner: ShellCommandRunner,
) {
    private val logger = LoggerFactory.getLogger(CommandService::class.java)

    suspend fun executeCommand(
        command: String,
        workDir: String? = null,
    ): CommandResult {
        val dir = workDir?.let { File(it) } ?: File(System.getProperty("user.dir"))

        if (!dir.exists()) {
            return CommandResult.failure("Working directory does not exist: $dir")
        }

        return shellCommandRunner.runShellCommand(command, dir)
    }

    suspend fun executeBatch(
        commands: List<String>,
        workDir: String? = null,
        stopOnError: Boolean = true,
    ): List<CommandResult> {
        val dir = workDir?.let { File(it) } ?: File(System.getProperty("user.dir"))

        if (!dir.exists()) {
            return listOf(CommandResult.failure("Working directory does not exist: $dir"))
        }

        logger.info("Executing batch of ${commands.size} commands")
        return shellCommandRunner.runBatchCommands(commands, dir, stopOnError)
    }
}
