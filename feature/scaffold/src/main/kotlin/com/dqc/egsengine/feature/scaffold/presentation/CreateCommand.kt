package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CreateCommand : CliktCommand(name = "create") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): CreateCommand =
            CreateCommand().subcommands(CreateModuleCommand(), CreateApiCommand())
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
            val dir = ProjectRootResolver.resolve(projectPath)

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

class CreateApiCommand : CliktCommand(name = "api"), KoinComponent {
    private val scaffolder: SwaggerApiScaffolder by inject()

    private val moduleName by argument(help = "Target feature module name, e.g. home")
    private val swaggerUrl by option("--swagger", "-s", help = "Swagger/OpenAPI json URL or file path")
        .default("")
    private val projectPath by option("--project", "-p", help = "Target project path")
        .default(".")
    private val packageName by option("--package", help = "Base package override, e.g. com.dqc.kango")
    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        if (swaggerUrl.isBlank()) {
            echo(CliFormatter.formatError("Missing --swagger option"), err = true)
            return
        }

        try {
            val dir = ProjectRootResolver.resolve(projectPath)
            val result = scaffolder.scaffold(
                projectRoot = dir,
                moduleName = moduleName,
                swaggerLocation = swaggerUrl,
                customPackage = packageName,
                dryRun = dryRun,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - swagger files to generate:"))
                result.files.forEach { echo("  $it") }
            } else {
                echo(CliFormatter.formatSuccess("Generated swagger API/domain scaffold for feature:$moduleName"))
                echo("  Generated files:")
                result.files.forEach { echo("    $it") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to generate from swagger: ${e.message}"), err = true)
        }
    }
}
