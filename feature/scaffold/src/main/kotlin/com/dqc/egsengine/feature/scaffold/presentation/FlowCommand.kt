package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ClientDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.SpringBootDatabaseScaffolder
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.koin.core.component.inject
import java.io.File

class FlowCommand : EgsCliCommand(name = "flow", help = "Cross-platform workflow orchestration") {
    override fun runCommand() = Unit

    companion object {
        fun withSubcommands(): FlowCommand = FlowCommand().subcommands(
            FlowAddFeatureCommand(),
            FlowCrudCommand(),
        )
    }
}

/**
 * `egs flow add-feature <module>` — client module scaffold + `client api sync` for the same module name.
 */
class FlowAddFeatureCommand : EgsCliCommand(name = "add-feature", help = "Add a feature module + sync API across platforms") {
    private val moduleScaffolder: ModuleScaffolder by inject()
    private val apiSync: ApiSyncScaffolder by inject()

    private val moduleName by argument(help = "Feature module name, e.g. todo, home")

    private val projectPath by option("--project", "-p").default(".")

    private val dryRun by option("--dry-run").flag()

    private val skipApiSync by option("--skip-api-sync", help = "Only create the feature module").flag()

    override fun runCommand() {
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
    }
}

/**
 * `egs flow crud --name <module> --ddl <file>` — full-stack CRUD: backend DDL → client API sync → client database.
 *
 * Orchestrates:
 * 1. Backend: SpringBoot database scaffold from DDL
 * 2. Client: API sync from backend Swagger
 * 3. Client: Database scaffold from same DDL
 */
class FlowCrudCommand : EgsCliCommand(name = "crud", help = "Full-stack CRUD: backend DDL + client API sync + client database") {
    private val springBootDatabaseScaffolder: SpringBootDatabaseScaffolder by inject()
    private val apiSync: ApiSyncScaffolder by inject()
    private val clientDatabaseScaffolder: ClientDatabaseScaffolder by inject()

    private val moduleName by argument(help = "Feature module name, e.g. todo, note")

    private val ddlFile by option("--ddl", help = "DDL .sql file with CREATE TABLE statements").required()

    private val projectPath by option("--project", "-p").default(".")

    private val dryRun by option("--dry-run", help = "Preview without writing files").flag()

    private val skipBackend by option("--skip-backend", help = "Skip backend database scaffold").flag()
    private val skipApiSync by option("--skip-api-sync", help = "Skip client API sync").flag()
    private val skipClientDb by option("--skip-client-db", help = "Skip client database scaffold").flag()

    override fun runCommand() {
        val workspaceRoot = ProjectRootResolver.resolve(projectPath)
        val sql = File(ddlFile)
        require(sql.exists()) { "DDL file not found: ${sql.absolutePath}" }

        val steps = mutableListOf<String>()
        if (!skipBackend) steps.add("backend database")
        if (!skipApiSync) steps.add("client api sync")
        if (!skipClientDb) steps.add("client database")
        val total = steps.size

        echo(CliFormatter.formatInfo("flow crud: $moduleName ($total steps)"))

        var step = 0

        if (!skipBackend) {
            step++
            echo(CliFormatter.formatInfo("$step/$total backend database: $moduleName"))
            val backendResult =
                springBootDatabaseScaffolder.scaffoldDatabase(
                    projectRoot = workspaceRoot,
                    sqlFile = sql,
                    moduleName = moduleName,
                    dryRun = dryRun,
                    force = false,
                )
            echo("   files: ${backendResult.files.size}")
        }

        if (!skipApiSync) {
            step++
            echo(CliFormatter.formatInfo("$step/$total client api sync: $moduleName"))
            val apiResult =
                apiSync.syncClientApi(
                    projectRoot = workspaceRoot,
                    clientModuleName = moduleName,
                    backendModuleName = moduleName,
                    swaggerUrl = null,
                    dryRun = dryRun,
                )
            echo("   generated: ${apiResult.files.size} files")
        }

        if (!skipClientDb) {
            step++
            echo(CliFormatter.formatInfo("$step/$total client database: $moduleName"))
            val clientResult =
                clientDatabaseScaffolder.scaffoldDatabase(
                    projectRoot = workspaceRoot,
                    sqlFile = sql,
                    moduleName = moduleName,
                    dryRun = dryRun,
                    repo = true,
                )
            echo("   files: ${clientResult.files.size}")
        }

        echo(CliFormatter.formatSuccess("flow crud completed for module '$moduleName'"))
    }
}
