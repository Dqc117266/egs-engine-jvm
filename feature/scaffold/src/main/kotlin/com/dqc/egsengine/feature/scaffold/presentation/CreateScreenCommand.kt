package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple as optionMultiple
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
 * egs create screen Login -m user -u FirstUseCase -u SecondUseCase
 * egs create screen Login -m user -u FirstUseCase SecondUseCase
 * ```
 */
class CreateScreenCommand : CliktCommand(name = "screen"), KoinComponent {

    private val pageScaffolder: PageScaffolder by inject()
    private val useCaseScanner: UseCaseScanner by inject()

    private val screenName by argument(
        name = "NAME",
        help = "Screen name, e.g. Login, Profile, Settings"
    )

    /**
     * Optional trailing names so `-u First Second` works: `-u` binds `First`, `Second` is collected here.
     */
    private val trailingUseCaseNames by argument(
        name = "ADDITIONAL_USECASES",
        help = "Additional use case names (optional; space-separated after the first `-u` value)",
    ).multiple(required = false)

    private val module by option(
        "-m", "--module",
        help = "Target feature module, e.g. user, home, profile"
    )

    private val useCaseOptions by option(
        "-u", "--usecase",
        help = "Use case class name(s); repeat -u, or comma/space-separated in one value, or `-u A B` (B as trailing arg)",
    ).optionMultiple()

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

    private val paging by option(
        "--paging",
        help = "Pagination UI codegen: auto (default), offset (Result<PageResult<T>>), paging3 (Flow<PagingData<T>>), none",
    ).default("auto")

    private val skipNav by option(
        "--skip-nav",
        help = "Do not edit app NavigationRoute / MainTemplateScreen",
    ).flag()

    private val skipAppWire by option(
        "--skip-app-wire",
        help = "Do not edit app build.gradle.kts or App.kt Koin modules",
    ).flag()

    private val withViewModelTest by option(
        "--with-test",
        help = "Also emit a minimal ViewModelTest skeleton (commonTest or test)",
    ).flag()

    override fun run() {
        try {
            val workspaceRoot = ProjectRootResolver.resolve(projectPath)
            val clientRoot = ProjectRootResolver.resolveGradleClientRoot(workspaceRoot)

            if (module != null) {
                runCommandMode(workspaceRoot, clientRoot)
            } else {
                runInteractiveMode(workspaceRoot, clientRoot)
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

    private fun runCommandMode(workspaceRoot: java.io.File, clientRoot: java.io.File) {
        val targetModule = module!!
        val name = validateScreenName(screenName)

        val modules = useCaseScanner.listModules(clientRoot)
        require(modules.contains(targetModule)) {
            "Module '$targetModule' not found. Available: ${modules.joinToString(", ")}"
        }

        val allUseCases = useCaseScanner.scanByModule(clientRoot, targetModule)
        val selectedUseCases = parseUseCases(collectedUseCaseNames(), allUseCases, targetModule)

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
            projectRoot = clientRoot,
            moduleName = targetModule,
            pageName = name,
            useCases = selectedUseCases,
            dryRun = dryRun,
            workspaceRoot = workspaceRoot,
            pagingOption = paging,
            skipNav = skipNav,
            skipAppWire = skipAppWire,
            withViewModelTest = withViewModelTest,
        )

        printResult(result)
    }

    private fun runInteractiveMode(workspaceRoot: java.io.File, clientRoot: java.io.File) {
        echo(CliFormatter.formatInfo("Create new screen"))
        echo()

        echo("Screen name: $screenName")
        val name = validateScreenName(screenName)
        echo()

        val modules = useCaseScanner.listModules(clientRoot)
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

        val availableUseCases = useCaseScanner.scanByModule(clientRoot, selectedModule)
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
            projectRoot = clientRoot,
            moduleName = selectedModule,
            pageName = name,
            useCases = selectedUseCases,
            dryRun = dryRun,
            workspaceRoot = workspaceRoot,
            pagingOption = paging,
            skipNav = skipNav,
            skipAppWire = skipAppWire,
            withViewModelTest = withViewModelTest,
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

    private fun collectedUseCaseNames(): List<String> {
        val fromOptions = useCaseOptions.flatMap { part ->
            part.split(Regex("[,\\s]+")).map { it.trim() }.filter { it.isNotBlank() }
        }
        val fromTrailing = trailingUseCaseNames.map { it.trim() }.filter { it.isNotBlank() }
        return fromOptions + fromTrailing
    }

    private fun parseUseCases(
        names: List<String>,
        allUseCases: List<UseCaseInfo>,
        moduleName: String,
    ): List<UseCaseInfo> {
        if (names.isEmpty()) return emptyList()

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
