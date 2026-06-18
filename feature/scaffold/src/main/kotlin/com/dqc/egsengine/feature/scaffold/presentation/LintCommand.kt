package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliError
import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

class LintCommand : EgsCliCommand(name = "lint", help = "Run lint checks on generated code") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): LintCommand = LintCommand().subcommands(
            LintFixCommand(),
        )
    }
}

class LintFixCommand : EgsCliCommand(name = "fix", help = "Auto-fix lint issues in generated code") {
    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    override fun runCommand() {
        // P0：代理到 Gradle 的 spotlessApply + detektApply（目标项目自己的 Gradle 任务）。
        val dir = File(projectPath).absoluteFile
        require(dir.isDirectory) { "Project path is not a directory: ${dir.absolutePath}" }

        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val gradlew = if (isWindows) "gradlew.bat" else "./gradlew"
        val cmd = listOf(gradlew, "spotlessApply", "detektApply")

        echo(CliFormatter.formatInfo("Running ${cmd.joinToString(" ")} in ${dir.absolutePath}"))
        // redirectErrorStream → 单流读取，避免 stdout/stderr 缓冲区死锁。
        val process = ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (output.isNotBlank()) echo(output)
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw CliError.GenericError("lint fix failed (gradlew exit $exitCode)")
        }
        echo(CliFormatter.formatSuccess("Lint fix complete"))
    }
}
