package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootCrudGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootGeneratedPathsManifest
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import com.dqc.egsengine.feature.scaffold.domain.AdminVueCrudScaffolder
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
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

class WebCommand : CliktCommand(name = "web") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): WebCommand = WebCommand().subcommands(
            WebCrudCommand.withSubcommands(),
        )
    }
}

class WebCrudCommand : CliktCommand(name = "crud") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): WebCrudCommand = WebCrudCommand().subcommands(
            WebCrudGenCommand(),
            WebCrudGenFromBackendCommand(),
        )
    }
}

/**
 * `egs web crud gen <module>` -- generates Vue3 CRUD pages for a module.
 */
class WebCrudGenCommand :
    CliktCommand(name = "gen"),
    KoinComponent {
    private val scaffolder: ModuleScaffolder by inject()

    private val name by argument(help = "Module name for CRUD generation")

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without creating files").flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)

            val result =
                scaffolder.scaffoldForProject(
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

/**
 * Admin CRUD from `feature/<module>/.egs-generated.json` `codegen`, or `--sql` + same DDL rules as backend.
 */
class WebCrudGenFromBackendCommand :
    CliktCommand(name = "gen-from-backend"),
    KoinComponent {
    private val adminVue: AdminVueCrudScaffolder by inject()
    private val manifest: SpringBootGeneratedPathsManifest by inject()
    private val crudGenerator: SpringBootCrudGenerator by inject()
    private val workspace: WorkspaceConfigResolver by inject()
    private val ddlParser: DdlParser by inject()

    private val moduleName by option(
        "--module",
        "-m",
        help = "Backend feature module name (feature/<module> holds .egs-generated.json)",
    ).required()

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without writing admin files").flag()

    private val sqlFile by option(
        "--sql",
        help = "DDL file to rebuild codegen when `.egs-generated.json` has no `codegen` section",
    )

    private val mainTable by option(
        "--main-table",
        help = "Table name when --sql defines multiple CREATE TABLE statements",
    )

    private val noAudit by option("--no-audit", help = "Match backend --no-audit when using --sql").flag()

    private val noSoftDelete by option("--no-soft-delete", help = "Match backend --no-soft-delete when using --sql").flag()

    private val noStatusEnum by option(
        "--no-status-enum",
        help = "Match backend --no-status-enum when using --sql",
    ).flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)
            val backendCfg = workspace.resolveBackend(dir)
            require(backendCfg.platform == Platform.SPRING_BOOT) {
                "web crud gen-from-backend requires workspace project 'backend' with platform spring_boot; got ${backendCfg.platform}"
            }
            val backendRoot = dir.resolve(backendCfg.path).normalize()

            var codegen =
                manifest.readCodegenOnly(backendRoot, moduleName)
            if (codegen == null) {
                val raw =
                    sqlFile
                        ?: error(
                            "No `codegen` in feature/$moduleName/.egs-generated.json; pass --sql <ddl> to derive it.",
                        )
                val sqlPath =
                    File(raw).let { path ->
                        if (path.isAbsolute) path else File(System.getProperty("user.dir")).resolve(path).normalize()
                    }
                val tables = ddlParser.parseFile(sqlPath)
                require(tables.isNotEmpty()) { "No CREATE TABLE statements in ${sqlPath.path}" }
                val table =
                    when {
                        mainTable != null ->
                            tables.singleOrNull { it.tableName.equals(mainTable, ignoreCase = true) }
                                ?: error(
                                    "Could not find table '$mainTable' in ${sqlPath.path}. Found: ${tables.map { it.tableName }}",
                                )
                        tables.size == 1 -> tables.first()
                        else ->
                            error(
                                "DDL defines ${tables.size} tables; pass --main-table=<name>. Tables: ${tables.map { it.tableName }}",
                            )
                    }
                val options =
                    SpringBootOpinionatedOptions(
                        auditColumns = !noAudit,
                        softDelete = !noSoftDelete,
                        statusEnum = !noStatusEnum,
                    )
                codegen =
                    crudGenerator.buildCodegenManifest(
                        table = table,
                        moduleName = moduleName,
                        config = backendCfg,
                        options = options,
                    )
            }

            val result =
                adminVue.scaffoldFromCodegen(
                    projectRoot = dir,
                    codegen = codegen,
                    dryRun = dryRun,
                )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run — admin module '${result.moduleName}' (${result.files.size} files):"))
                result.files.forEach { echo("  ${it.path}") }
                result.sysMenuFlywayMigration?.let {
                    echo(CliFormatter.formatInfo("Dry run — Flyway sys_menus: ${it.path}"))
                }
            } else {
                echo(
                    CliFormatter.formatSuccess(
                        "Admin Vue CRUD from backend manifest: module '${result.moduleName}' (${result.files.size} files)",
                    ),
                )
                result.files.forEach { echo("    ${it.path}") }
                result.sysMenuFlywayMigration?.let {
                    echo(CliFormatter.formatSuccess("Flyway sidebar migration: ${it.path}"))
                }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("gen-from-backend failed: ${e.message}"), err = true)
        }
    }
}
