package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClientCommand : CliktCommand(name = "client") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientCommand =
            ClientCommand().subcommands(
                ClientModuleCommand.withSubcommands(),
                ClientApiCommand.withSubcommands(),
            )
    }
}

// -- client module --

class ClientModuleCommand : CliktCommand(name = "module") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientModuleCommand =
            ClientModuleCommand().subcommands(
                ClientModuleCreateCommand(),
            )
    }
}

class ClientModuleCreateCommand : CliktCommand(name = "create"), KoinComponent {

    private val scaffolder: ModuleScaffolder by inject()

    private val name by argument(help = "Name of the feature module to create")

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)

            val result = scaffolder.scaffoldForProject(
                projectRoot = dir,
                moduleName = name,
                projectKey = "client",
                dryRun = dryRun,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - the following files would be created:"))
                echo()
                result.files.forEach { echo("  $it") }
            } else {
                echo(CliFormatter.formatSuccess("Created client module 'feature:$name'"))
                echo()
                echo("  Files created:")
                result.files.forEach { echo("    $it") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to create client module: ${e.message}"), err = true)
        }
    }
}

// -- client api --

class ClientApiCommand : CliktCommand(name = "api") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientApiCommand =
            ClientApiCommand().subcommands(
                ClientApiSyncCommand(),
            )
    }
}

/**
 * `egs client api sync <module>` or
 * `egs client api sync --client-module=X --backend-module=Y`
 */
class ClientApiSyncCommand : CliktCommand(name = "sync"), KoinComponent {

    private val apiSyncScaffolder: ApiSyncScaffolder by inject()

    private val moduleArg by argument(help = "Module name (shortcut for same-name sync)").optional()

    private val clientModuleOption by option(
        "--client-module",
        help = "Client feature module name",
    )

    private val backendModuleOption by option(
        "--backend-module",
        help = "Backend feature module name",
    )

    private val swaggerUrl by option(
        "--swagger", "-s",
        help = "Override Swagger JSON URL",
    )

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without writing files").flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)

            val clientModule = clientModuleOption ?: moduleArg
                ?: throw IllegalArgumentException("Module name required. Usage: egs client api sync <module> or --client-module=X --backend-module=Y")
            val backendModule = backendModuleOption ?: moduleArg
                ?: throw IllegalArgumentException("Backend module name required. Use --backend-module=Y or provide module name as argument")

            val result = apiSyncScaffolder.syncClientApi(
                projectRoot = dir,
                clientModuleName = clientModule,
                backendModuleName = backendModule,
                swaggerUrl = swaggerUrl,
                dryRun = dryRun,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - API sync preview:"))
                echo("  Client module: ${result.clientModule}")
                echo("  Backend module: ${result.backendModule}")
                echo("  Files:")
                result.files.forEach { echo("    ${it.path}") }
            } else {
                echo(CliFormatter.formatSuccess("API synced: ${result.backendModule} -> ${result.clientModule}"))
                echo("  Generated ${result.files.size} files")
                result.files.forEach { echo("    ${it.path}") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("API sync failed: ${e.message}"), err = true)
        }
    }
}
