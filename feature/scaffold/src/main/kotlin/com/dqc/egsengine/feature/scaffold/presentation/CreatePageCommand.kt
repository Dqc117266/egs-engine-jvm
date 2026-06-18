package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.PageScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.inject

/**
 * Create a page (interactive or non-interactive).
 *
 * Interactive:
 * ```
 * egs create page
 * ```
 * then follow prompts for module, name, UseCases.
 *
 * Non-interactive:
 * ```
 * egs create page --module home --name Profile --api GetUserPostsUseCase --api GetUserLevelUseCase
 * ```
 */
class CreatePageCommand : EgsCliCommand(name = "page", help = "Create a new Compose page/screen") {
    private val pageScaffolder: PageScaffolder by inject()
    private val useCaseScanner: UseCaseScanner by inject()

    private val module by option(
        "-m",
        "--module",
        help = "Target feature module, e.g. home",
    )

    private val pageName by option(
        "-n",
        "--name",
        help = "Page name, e.g. Profile",
    )

    private val apis by option(
        "-a",
        "--api",
        help = "UseCase name; can be repeated",
    ).multiple()

    private val projectPath by option(
        "-p",
        "--project",
        help = "Project root path",
    ).default(".")

    private val dryRun by option(
        "--dry-run",
        help = "Preview only; do not write files",
    ).flag()

    private val paging by option(
        "--paging",
        help = "Pagination UI codegen: auto, offset, paging3, none (default: auto)",
    ).default("auto")

    private val skipNav by option("--skip-nav", help = "Do not edit app NavigationRoute / MainTemplateScreen").flag()
    private val skipAppWire by option("--skip-app-wire", help = "Do not edit app Gradle / App.kt Koin").flag()

    private val withViewModelTest by option("--with-test", help = "Emit minimal ViewModelTest skeleton").flag()

    override fun runCommand() {
        val workspaceRoot = ProjectRootResolver.resolve(projectPath)
        val clientRoot = ProjectRootResolver.resolveGradleClientRoot(workspaceRoot)

        if (module != null && pageName != null) {
            runCommandMode(workspaceRoot, clientRoot)
        } else {
            runInteractiveMode(workspaceRoot, clientRoot)
        }
    }

    private fun runCommandMode(
        workspaceRoot: java.io.File,
        clientRoot: java.io.File,
    ) {
        val targetModule = module!!
        val name = pageName!!

        val modules = useCaseScanner.listModules(clientRoot)
        require(modules.contains(targetModule)) {
            "Module '$targetModule' not found. Available: ${modules.joinToString(", ")}"
        }

        val allUseCases = useCaseScanner.scanByModule(clientRoot, targetModule)
        val selectedUseCases =
            if (apis.isNotEmpty()) {
                apis.map { apiName ->
                    allUseCases.find { it.name == apiName || it.name == "${apiName}UseCase" }
                        ?: throw IllegalArgumentException("UseCase '$apiName' not found in module '$targetModule'")
                }
            } else {
                emptyList()
            }

        val result =
            pageScaffolder.scaffold(
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

    private fun runInteractiveMode(
        workspaceRoot: java.io.File,
        clientRoot: java.io.File,
    ) {
        echo(CliFormatter.formatInfo("Create new page"))
        echo()

        val modules = useCaseScanner.listModules(clientRoot)
        require(modules.isNotEmpty()) { "No feature modules found; create a module first" }

        echo("Select module:")
        modules.forEachIndexed { index, m ->
            echo("  [$index] $m")
        }
        echo()

        print("> Module index: ")
        val moduleIndex =
            readlnOrNull()?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid module selection")
        require(moduleIndex in modules.indices) { "Invalid module index" }

        val selectedModule = modules[moduleIndex]
        echo()

        print("> Page name (e.g. Profile): ")
        val name =
            readlnOrNull()?.trim()
                ?: throw IllegalArgumentException("Page name cannot be empty")
        require(name.isNotBlank()) { "Page name cannot be empty" }
        require(name.first().isLetter() && name.all { it.isLetterOrDigit() }) {
            "Page name must start with a letter and contain only letters and digits (e.g. taskDetail)"
        }
        echo()

        val availableUseCases = useCaseScanner.scanByModule(clientRoot, selectedModule)
        val selectedUseCases =
            if (availableUseCases.isNotEmpty()) {
                selectUseCasesInteractively(availableUseCases)
            } else {
                echo("No UseCases in module '$selectedModule'")
                emptyList()
            }

        echo()
        echo("Generation summary:")
        echo("   Module: $selectedModule")
        echo("   Page: $name")
        echo("   UseCases: ${if (selectedUseCases.isEmpty()) "(none)" else selectedUseCases.joinToString(", ") { it.name }}")
        echo()

        print("> Confirm? [Y/n]: ")
        val confirm = readLine()?.trim()?.lowercase() ?: "y"

        if (confirm == "n" || confirm == "no") {
            echo(CliFormatter.formatInfo("Cancelled"))
            return
        }

        echo()
        echo(CliFormatter.formatInfo("Generating..."))

        val result =
            pageScaffolder.scaffold(
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
                val indices =
                    input
                        .split(",", " ")
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
            echo(CliFormatter.formatSuccess("Page created successfully"))
            echo()
            echo("Generated files:")
            result.files.forEach { file ->
                echo("   ${CliFormatter.green("CREATE")} ${file.path}")
            }
            echo()
            echo("Next steps:")
            echo("   1. Add UI in the layout file")
            echo("   2. Bind data in renderState()")
            echo("   3. Handle side effects in handleEffect()")
            echo("   4. Add the page route in the navigation graph")
        }
    }
}
