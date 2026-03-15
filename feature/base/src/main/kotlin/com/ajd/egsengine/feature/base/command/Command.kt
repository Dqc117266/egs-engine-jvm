package com.ajd.egsengine.feature.base.command

interface Command {
    val name: String
    val description: String

    suspend fun execute(args: List<String>): CommandResult
}

data class CommandResult(
    val exitCode: Int,
    val output: String = "",
    val error: String = "",
) {
    val isSuccess: Boolean get() = exitCode == 0

    companion object {
        fun success(output: String = "") = CommandResult(exitCode = 0, output = output)
        fun failure(error: String, exitCode: Int = 1) = CommandResult(exitCode = exitCode, error = error)
    }
}
