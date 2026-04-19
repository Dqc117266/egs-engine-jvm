package com.dqc.egsengine.feature.scaffold.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.scaffold.domain.CreateUseCaseScaffolder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * `egs create usecase <Name> -m <module>` ¡ª generates a UseCase skeleton and repository stubs.
 */
class CreateUseCaseCommand : CliktCommand(name = "usecase"), KoinComponent {

    private val scaffolder: CreateUseCaseScaffolder by inject()

    private val name by argument(help = "Use case name without suffix, e.g. RefreshTodo")

    private val module by option("-m", "--module", help = "Feature module name").required()

    private val paged by option("--paged", help = "Generate paging-shaped invoke(page, pageSize)").flag()

    private val projectPath by option("--project", "-p").default(".")

    private val dryRun by option("--dry-run").flag()

    override fun run() {
        try {
            val workspaceRoot = ProjectRootResolver.resolve(projectPath)
            val clientRoot = ProjectRootResolver.resolveGradleClientRoot(workspaceRoot)
            val r = scaffolder.scaffold(
                clientRoot = clientRoot,
                workspaceRoot = workspaceRoot,
                moduleName = module,
                useCaseSimpleName = name,
                paged = paged,
                dryRun = dryRun,
            )
            if (dryRun) {
                echo(CliFormatter.formatInfo("Dry run: would create use case in feature/$module"))
            } else {
                echo(CliFormatter.formatSuccess("Created: ${r.files.joinToString { it.relativeTo(clientRoot).path }}"))
            }
        } catch (e: Exception) {
            echo(CliFormatter.formatError(e.message ?: "create usecase failed"), err = true)
        }
    }
}
