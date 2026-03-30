package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebCommand : CliktCommand(name = "web") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): WebCommand =
            WebCommand().subcommands(
                WebCrudCommand.withSubcommands(),
            )
    }
}

class WebCrudCommand : CliktCommand(name = "crud") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): WebCrudCommand =
            WebCrudCommand().subcommands(
                WebCrudGenCommand(),
            )
    }
}

/**
 * `egs web crud gen <module>` -- generates Vue3 CRUD pages for a module.
 */
class WebCrudGenCommand : CliktCommand(name = "gen"), KoinComponent {

    private val scaffolder: ModuleScaffolder by inject()

    private val name by argument(help = "Module name for CRUD generation")

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)

            val result = scaffolder.scaffoldForProject(
                projectRoot = dir,
                moduleName = name,
                projectKey = "admin",
                dryRun = dryRun,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - the following files would be generated:"))
                echo()
                result.files.forEach { echo("  $it") }
            } else {
                echo(CliFormatter.formatSuccess("Generated Vue3 CRUD for module '$name'"))
                echo()
                echo("  Files created:")
                result.files.forEach { echo("    $it") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to generate web CRUD: ${e.message}"), err = true)
        }
    }
}
