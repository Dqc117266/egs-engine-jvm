package com.dqc.egsengine.feature.analyzer.presentation

import com.dqc.egsengine.feature.analyzer.domain.ProjectAnalyzer
import com.dqc.egsengine.feature.analyzer.domain.model.ProjectInfo
import com.dqc.egsengine.feature.base.presentation.EgsCliCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.inject

class AnalyzeCommand : EgsCliCommand(name = "analyze") {
    private val analyzer: ProjectAnalyzer by inject()

    private val projectPath by argument().default(".")

    private val verbose by option("--verbose", "-v").flag()

    private val json by option("--json").flag()

    override fun runCommand() {
        val info = analyzer.analyze(projectPath)

        if (json) {
            echo(encodeAnalyzeJson(info))
        } else {
            echo(analyzer.generateSummary(info))
            if (verbose) {
                printVerboseDetails(info)
            }
        }
    }

    private fun printVerboseDetails(info: ProjectInfo) {
        echo()
        echo("  --- Detailed Module Information ---")
        echo()

        for (module in info.modules) {
            echo("  [${module.name}]")
            echo("    Type      : ${module.type.displayName}")
            echo("    Path      : ${module.path}")
            echo("    Manifest  : ${if (module.hasAndroidManifest) "Yes" else "No"}")

            if (module.plugins.isNotEmpty()) {
                echo("    Plugins   :")
                module.plugins.forEach { echo("      - $it") }
            }

            if (module.sourceSetDirs.isNotEmpty()) {
                echo("    SourceSets:")
                module.sourceSetDirs.forEach { echo("      - $it") }
            }

            if (module.dependencies.isNotEmpty()) {
                echo("    Deps      :")
                module.dependencies.forEach { echo("      - $it") }
            }

            echo()
        }
    }
}
