package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FlowCommand : CliktCommand(name = "flow") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): FlowCommand = FlowCommand().subcommands(FlowAddFeatureCommand())
    }
}

/**
 * `egs flow add-feature <module>` ¡ª client module scaffold + `client api sync` for the same module name.
 */
class FlowAddFeatureCommand :
    CliktCommand(name = "add-feature"),
    KoinComponent {
    private val moduleScaffolder: ModuleScaffolder by inject()
    private val apiSync: ApiSyncScaffolder by inject()

    private val moduleName by argument(help = "Feature module name, e.g. todo, home")

    private val projectPath by option("--project", "-p").default(".")

    private val dryRun by option("--dry-run").flag()

    private val skipApiSync by option("--skip-api-sync", help = "Only create the feature module").flag()

    override fun run() {
        try {
            val workspaceRoot = ProjectRootResolver.resolve(projectPath)
            echo(CliFormatter.formatInfo("1/2 client module create: $moduleName"))
            val mod =
                moduleScaffolder.scaffoldForProject(
                    projectRoot = workspaceRoot,
                    moduleName = moduleName,
                    projectKey = "client",
                    dryRun = dryRun,
                )
            echo("   files: ${mod.files.size}")
            if (!skipApiSync) {
                echo(CliFormatter.formatInfo("2/2 client api sync: $moduleName"))
                val api =
                    apiSync.syncClientApi(
                        projectRoot = workspaceRoot,
                        clientModuleName = moduleName,
                        backendModuleName = moduleName,
                        swaggerUrl = null,
                        dryRun = dryRun,
                    )
                echo("   generated: ${api.files.size} files")
            }
            echo(CliFormatter.formatSuccess("flow add-feature completed"))
        } catch (e: Exception) {
            echo(CliFormatter.formatError(e.message ?: "flow failed"), err = true)
        }
    }
}
