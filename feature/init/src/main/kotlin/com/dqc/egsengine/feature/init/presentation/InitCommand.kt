package com.dqc.egsengine.feature.init.presentation

import com.dqc.egsengine.feature.base.presentation.CliFormatter
import com.dqc.egsengine.feature.base.util.ProjectRootResolver
import com.dqc.egsengine.feature.init.domain.ProjectInitializer
import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InitCommand : CliktCommand(name = "init"), KoinComponent {

    private val initializer: ProjectInitializer by inject()

    private val projectPath by argument(help = "Path to the project to initialize").default(".")

    override fun run() {
        try {
            val dir = ProjectRootResolver.resolve(projectPath)

            echo("Initializing .egs for project at: ${dir.absolutePath}")
            echo()

            val config = initializer.initialize(dir)
            printSummary(config)

            echo()
            echo(CliFormatter.formatSuccess("Created .egs/config.json"))
        } catch (e: IllegalArgumentException) {
            echo(CliFormatter.formatError(e.message ?: "Invalid argument"), err = true)
        } catch (e: Exception) {
            echo(CliFormatter.formatError("Init failed: ${e.message}"), err = true)
        }
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
            val rows = config.baseClasses.map { bc ->
                listOf(bc.name, bc.module, bc.kind.name.lowercase().replace('_', ' '))
            }
            echo(CliFormatter.formatTable(headers, rows).prependIndent("    "))
        } else {
            echo()
            echo(CliFormatter.formatInfo("No base classes found"))
        }

        echo("=".repeat(50))
    }
}
