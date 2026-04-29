package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.dqc.egsengine.feature.scaffold.domain.SpringBootDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class BackendCommand : CliktCommand(name = "backend") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): BackendCommand =
            BackendCommand().subcommands(
                BackendModuleCommand.withSubcommands(),
                BackendGenCommand.withSubcommands(),
            )
    }
}

class BackendGenCommand : CliktCommand(name = "gen") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): BackendGenCommand =
            BackendGenCommand().subcommands(
                BackendGenDatabaseCommand(),
            )
    }
}

class BackendModuleCommand : CliktCommand(name = "module") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): BackendModuleCommand =
            BackendModuleCommand().subcommands(
                BackendModuleCreateCommand(),
            )
    }
}

class BackendModuleCreateCommand : CliktCommand(name = "create"), KoinComponent {

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
                projectKey = "backend",
                dryRun = dryRun,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - the following files would be created:"))
                echo()
                result.files.forEach { echo("  $it") }
            } else {
                echo(CliFormatter.formatSuccess("Created backend module 'feature:$name'"))
                echo()
                echo("  Files created:")
                result.files.forEach { echo("    $it") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to create backend module: ${e.message}"), err = true)
        }
    }
}

class BackendGenDatabaseCommand : CliktCommand(name = "database"), KoinComponent {

    private val scaffolder: SpringBootDatabaseScaffolder by inject()

    private val sqlFile by argument(help = "Path to SQL DDL file (CREATE TABLE)")

    private val moduleName by option(
        "--module",
        "-m",
        help = "Target backend feature module directory (feature/<module>)",
    ).required()

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview paths without writing").flag()

    private val force by option("--force", help = "Remove paths listed in `.egs-generated.json` before rewriting").flag()

    private val withAdmin by option(
        "--with-admin",
        help = "Also emit admin Vue CRUD (api/types/views/stores/router) from the same codegen manifest",
    ).flag()

    private val mainTable by option(
        "--main-table",
        help = "Table name when DDL contains multiple CREATE TABLE statements",
    )

    private val noAudit by option("--no-audit", help = "Do not apply JPA auditing/BaseEntity treatment for created_at/updated_at").flag()

    private val noSoftDelete by option("--no-soft-delete", help = "Ignore soft-delete column conventions").flag()

    private val noStatusEnum by option(
        "--no-status-enum",
        help = "Skip opinionated status-column handling (column still mapped if present)",
    ).flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)
            val sqlPath = File(sqlFile)
            val resolvedSql = if (sqlPath.isAbsolute) sqlPath else File(System.getProperty("user.dir")).resolve(sqlPath).normalize()

            val options = SpringBootOpinionatedOptions(
                auditColumns = !noAudit,
                softDelete = !noSoftDelete,
                statusEnum = !noStatusEnum,
            )

            val result = scaffolder.scaffoldDatabase(
                projectRoot = dir,
                sqlFile = resolvedSql,
                moduleName = moduleName,
                dryRun = dryRun,
                force = force,
                mainTable = mainTable,
                options = options,
                withAdmin = withAdmin,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run — ${result.tableName} → module '${result.moduleName}' (${result.files.size} files):"))
                result.files.forEach { echo("  ${it.path}") }
                if (result.adminFiles.isNotEmpty()) {
                    echo(CliFormatter.formatInfo("Dry run — admin (${result.adminFiles.size} files):"))
                    result.adminFiles.forEach { echo("  ${it.path}") }
                }
                result.sysMenuFlywayMigration?.let { m ->
                    echo(CliFormatter.formatInfo("Dry run — Flyway sys_menus (${m.path})"))
                }
            } else {
                echo(
                    CliFormatter.formatSuccess(
                        "Spring Boot database codegen: table '${result.tableName}', module '${result.moduleName}' (${result.files.size} files)",
                    ),
                )
                result.files.forEach { echo("    ${it.path}") }
                if (result.adminFiles.isNotEmpty()) {
                    echo(CliFormatter.formatSuccess("Admin Vue (${result.adminFiles.size} files)"))
                    result.adminFiles.forEach { echo("    ${it.path}") }
                }
                result.sysMenuFlywayMigration?.let { m ->
                    echo(CliFormatter.formatSuccess("Flyway sidebar migration: ${m.path}"))
                }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Database codegen failed: ${e.message}"), err = true)
        }
    }
}