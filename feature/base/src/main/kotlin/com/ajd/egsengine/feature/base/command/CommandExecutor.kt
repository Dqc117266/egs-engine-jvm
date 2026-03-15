package com.ajd.egsengine.feature.base.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

class CommandExecutor {
    private val logger = LoggerFactory.getLogger(CommandExecutor::class.java)

    suspend fun execute(
        command: List<String>,
        workDir: File = File(System.getProperty("user.dir")),
        environment: Map<String, String> = emptyMap(),
    ): CommandResult = withContext(Dispatchers.IO) {
        try {
            logger.debug("Executing: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
                .directory(workDir)
                .redirectErrorStream(false)

            environment.forEach { (key, value) ->
                processBuilder.environment()[key] = value
            }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            logger.debug("Exit code: $exitCode")

            CommandResult(
                exitCode = exitCode,
                output = output.trim(),
                error = error.trim(),
            )
        } catch (e: Exception) {
            logger.error("Command execution failed", e)
            CommandResult.failure("Execution failed: ${e.message}")
        }
    }
}
