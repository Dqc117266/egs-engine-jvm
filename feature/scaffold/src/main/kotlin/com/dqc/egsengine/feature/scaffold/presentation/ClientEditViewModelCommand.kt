/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.ViewModelEditScaffolder
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple as optionMultiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * `egs client edit viewmodel <page> -m <module> [-u UseCase ...]`
 */
class ClientEditViewModelCommand : CliktCommand(name = "viewmodel"), KoinComponent {

    private val scaffolder: ViewModelEditScaffolder by inject()
    private val useCaseScanner: UseCaseScanner by inject()

    private val pageName by argument(
        name = "PAGE",
        help = "Screen / page name (PascalCase), e.g. Profile, Login",
    )

    private val trailingUseCaseNames by argument(
        name = "ADDITIONAL_USECASES",
        help = "Additional use case names (optional; space-separated after the first -u value)",
    ).multiple(required = false)

    private val module by option(
        "-m",
        "--module",
        help = "Target feature module, e.g. user, home",
    ).required()

    private val useCaseOptions by option(
        "-u",
        "--usecase",
        help = "Use case class name(s); repeat -u, comma-separated, or `-u A B` (B as trailing arg)",
    ).optionMultiple()

    private val projectPath by option("--project", help = "Workspace / Gradle project root")
        .default(".")

    private val dryRun by option("--dry-run", help = "Print unified diff; do not write files").flag()

    private val paging by option(
        "--paging",
        help = "Must match screen: auto (default), offset, paging3, none",
    ).default("auto")

    override fun run() {
        try {
            val workspaceRoot = ProjectRootResolver.resolve(projectPath)
            val clientRoot = ProjectRootResolver.resolveGradleClientRoot(workspaceRoot)

            val modules = useCaseScanner.listModules(clientRoot)
            require(modules.contains(module)) {
                "Module '$module' not found. Available: ${modules.joinToString(", ")}"
            }

            val allUseCases = useCaseScanner.scanByModule(clientRoot, module)
            val names = collectedUseCaseNames()
            val selected =
                if (names.isNotEmpty()) {
                    parseUseCases(names, allUseCases, module)
                } else {
                    if (allUseCases.isEmpty()) {
                        throw IllegalArgumentException("No use cases in module '$module'")
                    }
                    echo(CliFormatter.formatInfo("Select use cases to wire (same as create screen)"))
                    selectUseCasesInteractively(allUseCases).also {
                        if (it.isEmpty()) {
                            throw IllegalArgumentException("No use cases selected")
                        }
                    }
                }
            val pascal = pageName.replaceFirstChar { it.uppercase() }

            echo()
            echo(CliFormatter.formatInfo("Edit viewmodel summary:"))
            echo("   Page: $pascal")
            echo("   Module: $module")
            echo("   UseCases: ${selected.joinToString(", ") { it.name }}")
            echo()

            val result =
                scaffolder.edit(
                    projectRoot = clientRoot,
                    moduleName = module,
                    pageName = pascal,
                    selectedUseCases = selected,
                    dryRun = dryRun,
                    workspaceRoot = workspaceRoot,
                    pagingOption = paging,
                )

            val pr = result.pageScaffoldResult
            val st = result.stats

            if (dryRun) {
                if (result.diffs.isEmpty()) {
                    echo(CliFormatter.formatInfo("Nothing to change (all selected use cases already wired)."))
                } else {
                    echo(CliFormatter.formatInfo("Dry run ¡ª unified diff:"))
                    echo()
                    result.diffs.forEach { (path, diff) ->
                        echo(CliFormatter.formatInfo("--- $path ---"))
                        echo(diff)
                        echo()
                    }
                }
            }

            echo(
                CliFormatter.formatInfo(
                    "Added: ${st.ctorParams} ctor params, ${st.intents} intents, ${st.stateFields} state fields, " +
                        "${st.registerBlocks} registerIntent blocks, ${st.handlers} handlers; " +
                        "${st.contractImports} contract imports, ${st.viewModelImports} ViewModel imports.",
                ),
            )

            if (!dryRun) {
                if (pr.files.isEmpty()) {
                    echo(CliFormatter.formatInfo("No files were modified."))
                } else {
                    echo(CliFormatter.formatSuccess("Updated ${pr.files.size} file(s)"))
                    pr.files.forEach { echo("   ${it.path}") }
                }
            } else if (pr.files.isNotEmpty()) {
                echo(CliFormatter.formatInfo("(Remove --dry-run to apply)"))
                pr.files.forEach { echo("   would write: ${it.path}") }
            }
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("client edit viewmodel failed: ${e.message}"), err = true)
        }
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
    ): List<UseCaseInfo> =
        names.map { name ->
            allUseCases.find { it.name == name || it.name == "${name}UseCase" }
                ?: throw IllegalArgumentException("UseCase '$name' not found in module '$moduleName'")
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
        val input = readlnOrNull()?.trim() ?: ""

        return when {
            input.isEmpty() -> emptyList()
            input == "a" || input == "all" -> useCases
            else -> {
                val indices =
                    input.split(",", " ")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in useCases.indices }
                indices.map { useCases[it] }
            }
        }
    }
}

class ClientEditCommand : CliktCommand(name = "edit") {
    override fun run() = Unit

    companion object {
        fun withSubcommands(): ClientEditCommand =
            ClientEditCommand().subcommands(
                ClientEditViewModelCommand(),
            )
    }
}
