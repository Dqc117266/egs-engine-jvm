package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.swagger.SwaggerApiScaffolder
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.inject

class CreateCommand : EgsCliCommand(name = "create") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): CreateCommand = CreateCommand().subcommands(
            CreateProjectCommand(),
            CreateModuleCommand(),
            CreateApiCommand(),
            CreateScreenCommand(),
            CreatePageCommand(), // backward compatibility
            CreateUseCaseCommand(),
        )
    }
}

class CreateModuleCommand : EgsCliCommand(name = "module") {
    private val scaffolder: ModuleScaffolder by inject()

    private val name by argument(help = "Name of the feature module to create")

    private val projectPath by option("--project", "-p", help = "Target project path")
        .default(".")

    private val packageName by option("--package", help = "Custom package name")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun runCommand() {
        val dir = ProjectRootResolver.resolve(projectPath)

        // Try workspace-aware scaffold first (client sub-project)
        val workspaceFile = dir.resolve(".egs/workspace.json")
        val result =
            if (workspaceFile.exists()) {
                scaffolder.scaffoldForProject(
                    projectRoot = dir,
                    moduleName = name,
                    projectKey = "client",
                    dryRun = dryRun,
                )
            } else {
                scaffolder.scaffold(
                    projectRoot = dir,
                    moduleName = name,
                    customPackage = packageName,
                    dryRun = dryRun,
                )
            }

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
    }
}

class CreateApiCommand : EgsCliCommand(name = "api") {
    private val scaffolder: SwaggerApiScaffolder by inject()

    private val moduleName by argument(help = "Target feature module name, e.g. home")
    private val swaggerUrl by option("--swagger", "-s", help = "Swagger/OpenAPI json URL or file path")
        .default("")
    private val projectPath by option("--project", "-p", help = "Target project path")
        .default(".")
    private val packageName by option("--package", help = "Base package override, e.g. com.dqc.kango")
    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun runCommand() {
        require(swaggerUrl.isNotBlank()) { "Missing --swagger option" }

        val dir = ProjectRootResolver.resolve(projectPath)
        val result =
            scaffolder.scaffold(
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
    }
}
