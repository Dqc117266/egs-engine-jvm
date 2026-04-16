package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Create a Compose screen (full CLI + interactive).
 *
 * Example:
 * ```
 * egs create screen Login \
 *   --module user \
 *   --usecase DefaultAi4043UseCase,TopicCreateTopicUseCase \
 *   --route "user/login" \
 *   --params "email:String,password:String"
 * ```
 *
 * Short form:
 * ```
 * egs create screen Login -m user -u DefaultAi4043UseCase,TopicCreateTopicUseCase
 * ```
 */
class CreateScreenCommand : CliktCommand(name = "screen"), KoinComponent {

    private val pageScaffolder: PageScaffolder by inject()
    private val useCaseScanner: UseCaseScanner by inject()

    private val screenName by argument(
        name = "NAME",
        help = "Screen name, e.g. Login, Profile, Settings"
    )

    private val module by option(
        "-m", "--module",
        help = "Target feature module, e.g. user, home, profile"
    )

    private val useCases by option(
        "-u", "--usecase",
        help = "Comma-separated UseCase names, e.g. GetUserUseCase,UpdateUserUseCase"
    )

    private val route by option(
        "-r", "--route",
        help = "Navigation path, e.g. user/login, profile/{id}"
    )

    private val params by option(
        "-p", "--params",
        help = "Screen args as name:Type pairs, comma-separated, e.g. email:String,password:String"
    )

    private val projectPath by option(
        "--project",
        help = "Project root path"
    ).default(".")

    private val dryRun by option(
        "--dry-run",
        help = "Preview only; do not write files"
    ).flag()

    override fun run() {
        try {
            val projectRoot = ProjectRootResolver.resolve(projectPath)

            if (module != null) {
                runCommandMode(projectRoot)
            } else {
                runInteractiveMode(projectRoot)
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Screen generation failed: ${e.message}"), err = true)
            if (System.getenv("EGS_DEBUG") == "true") {
                e.printStackTrace()
            }
        }
    }

    private fun runCommandMode(projectRoot: java.io.File) {
        val targetModule = module!!
        val name = validateScreenName(screenName)

        val modules = useCaseScanner.listModules(projectRoot)
        require(modules.contains(targetModule)) {
            "Module '$targetModule' not found. Available: ${modules.joinToString(", ")}"
        }

        val allUseCases = useCaseScanner.scanByModule(projectRoot, targetModule)
        val selectedUseCases = parseUseCases(useCases, allUseCases, targetModule)

        val screenParams = parseParams(params)

        echo()
        echo(CliFormatter.formatInfo("Generation summary:"))
        echo("   Screen name: $name")
        echo("   Module: $targetModule")
        echo("   UseCases: ${selectedUseCases.joinToString(", ") { it.name }.ifEmpty { "(none)" }}")
        if (route != null) echo("   Route: $route")
        if (screenParams.isNotEmpty()) echo("   Params: ${screenParams.joinToString(", ") { "${it.first}: ${it.second}" }}")
        echo()

        val result = pageScaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = targetModule,
            pageName = name,
            useCases = selectedUseCases,
            dryRun = dryRun,
        )

        printResult(result)
    }

    private fun runInteractiveMode(projectRoot: java.io.File) {
        echo(CliFormatter.formatInfo("Create new screen"))
        echo()

        echo("Screen name: $screenName")
        val name = validateScreenName(screenName)
        echo()

        val modules = useCaseScanner.listModules(projectRoot)
        if (modules.isEmpty()) {
            throw IllegalArgumentException("No feature modules found; create a module first")
        }

        echo("Select target module:")
        modules.forEachIndexed { index, m ->
            echo("  [$index] $m")
        }
        echo()

        print("> Module index: ")
        val moduleIndex = readlnOrNull()?.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid module selection")
        require(moduleIndex in modules.indices) { "Invalid module index" }

        val selectedModule = modules[moduleIndex]
        echo()

        val availableUseCases = useCaseScanner.scanByModule(projectRoot, selectedModule)
        val selectedUseCases = if (availableUseCases.isNotEmpty()) {
            selectUseCasesInteractively(availableUseCases)
        } else {
            echo("No UseCases in module '$selectedModule'")
            emptyList()
        }

        echo()
        print("> Navigation route (optional, e.g. user/login): ")
        val routePath = readlnOrNull()?.trim()?.takeIf { it.isNotEmpty() }

        echo()
        echo(CliFormatter.formatInfo("Generation summary:"))
        echo("   Screen name: $name")
        echo("   Module: $selectedModule")
        echo("   UseCases: ${selectedUseCases.joinToString(", ") { it.name }.ifEmpty { "(none)" }}")
        if (routePath != null) echo("   Route: $routePath")
        echo()

        print("> Confirm? [Y/n]: ")
        val confirm = readLine()?.trim()?.lowercase() ?: "y"

        if (confirm == "n" || confirm == "no") {
            echo(CliFormatter.formatInfo("Cancelled"))
            return
        }

        echo()
        echo(CliFormatter.formatInfo("Generating..."))

        val result = pageScaffolder.scaffold(
            projectRoot = projectRoot,
            moduleName = selectedModule,
            pageName = name,
            useCases = selectedUseCases,
            dryRun = dryRun,
        )

        printResult(result)
    }

    private fun validateScreenName(name: String): String {
        require(name.isNotBlank()) { "Screen name cannot be empty" }
        require(name.first().isLetter() && name.all { it.isLetterOrDigit() }) {
            "Screen name must start with a letter and contain only letters and digits (camelCase/PascalCase)"
        }
        return name.replaceFirstChar { it.uppercase() }
    }

    private fun parseUseCases(
        useCaseStr: String?,
        allUseCases: List<UseCaseInfo>,
        moduleName: String
    ): List<UseCaseInfo> {
        if (useCaseStr.isNullOrBlank()) return emptyList()

        val names = useCaseStr.split(",").map { it.trim() }
        return names.map { name ->
            allUseCases.find { it.name == name || it.name == "${name}UseCase" }
                ?: throw IllegalArgumentException("UseCase '$name' not found in module '$moduleName'")
        }
    }

    private fun parseParams(paramsStr: String?): List<Pair<String, String>> {
        if (paramsStr.isNullOrBlank()) return emptyList()

        return paramsStr.split(",").map { param ->
            val parts = param.trim().split(":")
            require(parts.size == 2) { "Bad param format: $param (expected name:Type)" }
            parts[0].trim() to parts[1].trim()
        }
    }

    private fun selectUseCasesInteractively(useCases: List<UseCaseInfo>): List<UseCaseInfo> {
        echo("Select UseCases (comma-separated indices, or 'a' for all):")
        useCases.forEachIndexed { index, uc ->
            echo("  [$index] ${uc.name}")
        }
        echo("  [a] all")
        echo("  [enter] skip")
        echo()

        print("> Choice (e.g. 0,2 or a): ")
        val input = readLine()?.trim() ?: ""

        return when {
            input.isEmpty() -> emptyList()
            input == "a" || input == "all" -> useCases
            else -> {
                val indices = input.split(",", " ")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in useCases.indices }
                indices.map { useCases[it] }
            }
        }
    }

    private fun printResult(result: com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult) {
        echo()

        if (result.dryRun) {
            echo(CliFormatter.formatInfo("Dry run - files to create:"))
            echo()
            result.files.forEach { file ->
                echo("   CREATE: ${file.path}")
            }
            echo()
            echo(CliFormatter.formatInfo("(Remove --dry-run to write files)"))
        } else {
            echo(CliFormatter.formatSuccess("Screen created successfully"))
            echo()
            echo("Generated files:")
            result.files.forEach { file ->
                echo("   ${CliFormatter.green("CREATE")} ${file.path}")
            }
            echo()
            echo("Next steps:")
            echo("   1. Add UI in the Screen file")
            echo("   2. Define state in Contract")
            echo("   3. Implement logic in ViewModel")
            echo("   4. Register route in NavigationRoute")
        }
    }
}
