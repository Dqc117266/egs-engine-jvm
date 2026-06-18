package com.dqc.egsengine.feature.init.presentation

import com.dqc.egsengine.feature.base.presentation.CliError
import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.init.domain.ProjectInitializer
import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import org.koin.core.component.inject
import java.io.File

class InitCommand : EgsCliCommand(name = "init", help = "Initialize a new .egs project configuration") {
    private val initializer: ProjectInitializer by inject()
    private val workspaceConfigReader: WorkspaceConfigReader by inject()

    private val projectPath by argument(help = "Path to the project to initialize").default(".")

    override fun runCommand() {
        val resolvedRoot = ProjectRootResolver.resolve(projectPath)
        val dir = resolveInitGradleRoot(resolvedRoot)

        echo("Initializing .egs for project at: ${dir.absolutePath}")
        if (dir != resolvedRoot) {
            echo(CliFormatter.formatInfo("(workspace root: ${resolvedRoot.absolutePath})"))
        }
        echo()

        val config = initializer.initialize(dir)
        printSummary(config)

        echo()
        echo(CliFormatter.formatSuccess("Created .egs/config.json"))
    }

    private fun printSummary(config: EgsConfig) {
        echo("=".repeat(50))
        echo("  Project: ${config.projectName}")
        echo("  Type   : ${config.projectType}")
        echo("  Path   : ${config.rootPath}")

        config.conventionPluginId?.let {
            echo("  Plugin : $it")
        }
        config.basePackage?.let {
            echo("  Package: $it")
        }

        echo()
        echo("  Module Structure:")
        echo("    Layers : ${config.moduleStructure.layers.joinToString(", ")}")
        echo("    Has res: ${config.moduleStructure.hasRes}")

        if (config.baseClasses.isNotEmpty()) {
            echo()
            echo("  Base Classes (${config.baseClasses.size}):")

            val headers = listOf("Name", "Module", "Kind")
            val rows =
                config.baseClasses.map { bc ->
                    listOf(
                        bc.name,
                        bc.module,
                        bc.kind.name
                            .lowercase()
                            .replace('_', ' '),
                    )
                }
            echo(CliFormatter.formatTable(headers, rows).prependIndent("    "))
        } else {
            echo()
            echo(CliFormatter.formatInfo("No base classes found"))
        }

        echo("=".repeat(50))
    }

    /**
     * [ProjectRootResolver] may point at a multi-project workspace (only `.egs/workspace.json`).
     * [ProjectInitializer] needs a directory that contains `settings.gradle*`.
     */
    private fun resolveInitGradleRoot(resolvedRoot: File): File {
        if (ProjectRootResolver.hasGradleSettings(resolvedRoot)) return resolvedRoot

        if (!workspaceConfigReader.hasWorkspaceConfig(resolvedRoot)) {
            throw CliError.UsageError(
                "Not a Gradle project (no settings.gradle): ${resolvedRoot.absolutePath}. " +
                    "If you use `new project`, run init on the client app, e.g. " +
                    "-p ${resolvedRoot.resolve("client").absolutePath}",
            )
        }

        val workspace = workspaceConfigReader.read(resolvedRoot)
        workspace.projects["client"]?.let { sub ->
            val clientRoot = resolvedRoot.resolve(sub.path)
            if (ProjectRootResolver.hasGradleSettings(clientRoot)) return clientRoot
        }
        for ((_, sub) in workspace.projects) {
            val subRoot = resolvedRoot.resolve(sub.path)
            if (ProjectRootResolver.hasGradleSettings(subRoot)) return subRoot
        }
        throw CliError.UsageError(
            "Workspace at ${resolvedRoot.absolutePath} has no Gradle subproject with settings.gradle. " +
                "Projects in workspace.json: ${workspace.projects.keys.joinToString()}. " +
                "Clone/finish template setup, then run init with -p pointing at that subfolder.",
        )
    }
}
