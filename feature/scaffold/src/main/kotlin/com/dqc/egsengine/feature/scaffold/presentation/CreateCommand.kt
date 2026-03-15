package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class CreateCommand : CliktCommand(name = "create") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): CreateCommand =
            CreateCommand().subcommands(CreateModuleCommand())
    }
}

class CreateModuleCommand : CliktCommand(name = "module"), KoinComponent {

    private val scaffolder: ModuleScaffolder by inject()

    private val name by argument(help = "Name of the feature module to create")

    private val projectPath by option("--project", "-p", help = "Target project path")
        .default(".")

    private val packageName by option("--package", help = "Custom package name")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        try {
            val dir = File(projectPath).let {
                if (it.isAbsolute) it else it.absoluteFile
            }

            val result = scaffolder.scaffold(
                projectRoot = dir,
                moduleName = name,
                customPackage = packageName,
                dryRun = dryRun,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - the following files would be created:"))
                echo()
                result.files.forEach { echo("  $it") }
                echo()
                echo("  settings.gradle.kts would be updated with :feature:$name")
            } else {
                echo(CliFormatter.formatSuccess("Created module 'feature:$name'"))
                echo()
                echo("  Files created:")
                result.files.forEach { echo("    $it") }
                echo()
                echo("  settings.gradle.kts updated with :feature:$name")
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to create module: ${e.message}"), err = true)
        }
    }
}
