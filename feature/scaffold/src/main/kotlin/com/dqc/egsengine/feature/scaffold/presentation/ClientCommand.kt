package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.ApiSyncScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpDatabaseScaffolder
import com.dqc.egsengine.feature.scaffold.domain.KmpPreferencesScaffolder
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.ModuleScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class ClientCommand : CliktCommand(name = "client") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientCommand =
            ClientCommand().subcommands(
                ClientModuleCommand.withSubcommands(),
                ClientListCommand.withSubcommands(),
                ClientApiCommand.withSubcommands(),
                ClientGenCommand.withSubcommands(),
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

            val clientRoot = ProjectRootResolver.resolveGradleClientRoot(dir)
            val moduleRoot = clientRoot.resolve("feature/$name")
            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run - the following files would be created:"))
                echo()
                echo(CliFormatter.formatInfo("  Client project (Gradle root): ${clientRoot.absolutePath}"))
                echo(CliFormatter.formatInfo("  Module directory: ${moduleRoot.absolutePath}"))
                echo()
                result.files.forEach { echo("  $it") }
            } else {
                echo(CliFormatter.formatSuccess("Created client module 'feature:$name'"))
                echo()
                echo("  Client project (Gradle root): ${clientRoot.absolutePath}")
                echo("  Module directory: ${moduleRoot.absolutePath}")
                echo()
                echo("  Files created (paths relative to client project):")
                result.files.forEach { echo("    $it") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to create client module: ${e.message}"), err = true)
        }
    }
}

// -- client list --

class ClientListCommand : CliktCommand(name = "list") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientListCommand =
            ClientListCommand().subcommands(
                ClientListUsecasesCommand(),
            )
    }
}

/**
 * `egs-engine client list usecases [--module=X] [--project=P]`
 *
 * Lists `*UseCase.kt` under each `feature/<module>`, grouped by module (git-status style).
 */
class ClientListUsecasesCommand : CliktCommand(name = "usecases"), KoinComponent {

    private val scanner: UseCaseScanner by inject()

    private val module by option(
        "-m",
        "--module",
        help = "Only list use cases in this feature module",
    )

    private val projectPath by option("--project", "-p", help = "Workspace or Gradle project root")
        .default(".")

    override fun run() {
        try {
            val workspaceRoot = ProjectRootResolver.resolve(projectPath)
            val clientRoot = ProjectRootResolver.resolveGradleClientRoot(workspaceRoot)

            if (module != null) {
                val target = module!!
                val modules = scanner.listModules(clientRoot)
                require(modules.contains(target)) {
                    "Module '$target' not found. Available: ${modules.joinToString(", ").ifEmpty { "(none)" }}"
                }
                val useCases = scanner.scanByModule(clientRoot, target)
                echo(CliFormatter.formatInfo("Client root: ${clientRoot.absolutePath}"))
                echo()
                if (useCases.isEmpty()) {
                    echo("feature/$target")
                    echo("  (no *UseCase.kt files)")
                    return
                }
                echo("feature/$target")
                useCases.forEach { uc ->
                    echo("  ${uc.name}")
                    echo("    ${uc.path}")
                }
                return
            }

            val modules = scanner.listModules(clientRoot)
            echo(CliFormatter.formatInfo("Client root: ${clientRoot.absolutePath}"))
            echo()
            if (modules.isEmpty()) {
                echo("(no feature modules under feature/)")
                return
            }

            var anyPrinted = false
            for (m in modules) {
                val useCases = scanner.scanByModule(clientRoot, m)
                if (useCases.isEmpty()) continue
                anyPrinted = true
                echo("feature/$m")
                useCases.forEach { uc ->
                    echo("  ${uc.name}")
                    echo("    ${uc.path}")
                }
                echo()
            }
            if (!anyPrinted) {
                echo("(no *UseCase.kt files in any feature module)")
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Failed to list use cases: ${e.message}"), err = true)
        }
    }
}

// -- client gen --

class ClientGenCommand : CliktCommand(name = "gen") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientGenCommand =
            ClientGenCommand().subcommands(
                ClientGenDatabaseCommand(),
                ClientGenPrefsCommand(),
            )
    }
}

/**
 * `egs client gen database <sql-file> --module=X`
 */
class ClientGenDatabaseCommand : CliktCommand(name = "database"), KoinComponent {

    private val scaffolder: KmpDatabaseScaffolder by inject()

    private val sqlFile by argument(help = "Path to SQL DDL file (CREATE TABLE)")

    private val moduleName by option(
        "--module",
        "-m",
        help = "Target KMP feature module name (under feature/<module>)",
    ).required()

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without writing files").flag()

    private val repo by option(
        "--repo",
        help = "Generate repository layer (Mode A: DB-only, Mode B: inject DB into API repository when api sync exists)",
    ).flag()

    private val cached by option(
        "--cached",
        help = "With --repo and existing API sync: cache-aside GETs + entity mappers (implies --repo)",
    ).flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)
            val sqlPath = File(sqlFile)
            val resolvedSql = if (sqlPath.isAbsolute) sqlPath else File(System.getProperty("user.dir")).resolve(sqlPath).normalize()

            val effectiveRepo = repo || cached
            val result = scaffolder.scaffoldDatabase(
                projectRoot = dir,
                sqlFile = resolvedSql,
                moduleName = moduleName,
                dryRun = dryRun,
                repo = effectiveRepo,
                cached = cached,
            )

            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run ¡ª KMP database codegen preview:"))
                echo("  Module: ${result.moduleName}")
                echo("  Files:")
                result.files.forEach { echo("    ${it.path}") }
            } else {
                echo(CliFormatter.formatSuccess("Generated Room database sources for module '${result.moduleName}'"))
                echo("  ${result.files.size} files")
                result.files.forEach { echo("    ${it.path}") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Database codegen failed: ${e.message}"), err = true)
        }
    }
}

/**
 * `egs client gen prefs --module=X --fields=... [--key=Y]`
 */
class ClientGenPrefsCommand : CliktCommand(name = "prefs"), KoinComponent {

    private val scaffolder: KmpPreferencesScaffolder by inject()

    private val moduleName by option(
        "--module",
        "-m",
        help = "Target KMP feature module name (under feature/<module>)",
    ).required()

    private val fields by option(
        "--fields",
        "--feilds",
        help = "Comma-separated fields: name:type (String, Boolean/bool, Int, Long)",
    ).required()

    private val key by option(
        "--key",
        "-k",
        help = "Logical key: for a single field, names preference consts; for multiple fields, snapshot key + model name",
    )

    private val projectPath by option("--project", "-p", help = "Workspace root path")
        .default(".")

    private val dryRun by option("--dry-run", help = "Preview without writing files").flag()

    private val force by option(
        "--force",
        help = "Overwrite snapshot model / duplicate keys (MVP: snapshot should usually be generated once per --key)",
    ).flag()

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)
            val result = scaffolder.scaffoldPrefs(
                projectRoot = dir,
                moduleName = moduleName,
                fieldsArg = fields,
                keyArg = key,
                dryRun = dryRun,
                force = force,
            )
            if (result.dryRun) {
                echo(CliFormatter.formatInfo("Dry run — KMP preferences codegen preview:"))
                echo("  Module: ${result.moduleName}")
                echo("  Files:")
                result.files.forEach { echo("    ${it.path}") }
            } else {
                echo(CliFormatter.formatSuccess("Generated preferences for module '${result.moduleName}'"))
                echo("  ${result.files.size} files")
                result.files.forEach { echo("    ${it.path}") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: IllegalStateException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid state"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Preferences codegen failed: ${e.message}"), err = true)
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

    /** Same value for both client and backend when `--client-module` / `--backend-module` are not set. */
    private val moduleOption by option(
        "--module",
        "-m",
        help = "Shortcut: use the same name for both client and backend modules",
    )

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

            // Priority: explicit --client-module / --backend-module > --module > positional argument
            val clientModule = clientModuleOption ?: moduleOption ?: moduleArg
                ?: throw IllegalArgumentException(
                    "Module name required. Usage: egs client api sync <module> or --module=X or --client-module=X [--backend-module=Y]",
                )
            val backendModule = backendModuleOption ?: moduleOption ?: moduleArg
                ?: throw IllegalArgumentException(
                    "Backend module name required. Use --backend-module=Y, --module=X, or positional <module>",
                )

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
