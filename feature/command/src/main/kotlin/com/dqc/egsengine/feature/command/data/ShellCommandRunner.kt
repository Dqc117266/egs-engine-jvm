package com.dqc.egsengine.feature.command.data

import com.dqc.egsengine.feature.base.command.CommandExecutor
import com.dqc.egsengine.feature.base.command.CommandResult
import org.slf4j.LoggerFactory
import java.io.File

class ShellCommandRunner(
    private val commandExecutor: CommandExecutor,
) {
    private val logger = LoggerFactory.getLogger(ShellCommandRunner::class.java)

    suspend fun runShellCommand(
        command: String,
        workDir: File = File(System.getProperty("user.dir")),
    ): CommandResult {
        logger.info("Running shell command: $command")

        val shellCommand = if (isWindows()) {
            listOf("cmd", "/c", command)
        } else {
            listOf("sh", "-c", command)
        }

        return commandExecutor.execute(shellCommand, workDir)
    }

    suspend fun runBatchCommands(
        commands: List<String>,
        workDir: File = File(System.getProperty("user.dir")),
        stopOnError: Boolean = true,
    ): List<CommandResult> {
        val results = mutableListOf<CommandResult>()

        for (command in commands) {
            val result = runShellCommand(command, workDir)
            results.add(result)

            if (!result.isSuccess && stopOnError) {
                logger.warn("Stopping batch execution due to error in: $command")
                break
            }
        }

        return results
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("win")
}
