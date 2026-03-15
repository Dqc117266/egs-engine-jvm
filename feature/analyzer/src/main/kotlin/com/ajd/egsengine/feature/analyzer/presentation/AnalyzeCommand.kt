package com.ajd.egsengine.feature.analyzer.presentation

import com.ajd.egsengine.feature.analyzer.domain.ProjectAnalyzer
import com.ajd.egsengine.feature.analyzer.domain.model.ProjectInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AnalyzeCommand : CliktCommand(name = "analyze"), KoinComponent {

    private val analyzer: ProjectAnalyzer by inject()

    private val projectPath by argument().default(".")

    private val verbose by option("--verbose", "-v").flag()

    private val json by option("--json").flag()

    override fun run() {
        try {
            val info = analyzer.analyze(projectPath)

            if (json) {
                echo(toJson(info))
            } else {
                echo(analyzer.generateSummary(info))
                if (verbose) {
                    printVerboseDetails(info)
                }
            }
        } catch (e: IllegalArgumentException) {
            echo("Error: ${e.message}", err = true)
        } catch (e: Exception) {
            echo("Analysis failed: ${e.message}", err = true)
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

    private fun toJson(info: ProjectInfo): String = buildString {
        appendLine("{")
        appendLine("""  "name": "${info.name}",""")
        appendLine("""  "rootPath": "${info.rootPath.replace("\\", "\\\\")}",""")
        appendLine("""  "gradleVersion": "${info.gradleVersion}",""")
        appendLine("""  "kotlinVersion": ${info.kotlinVersion?.let { "\"$it\"" } ?: "null"},""")
        appendLine("""  "javaVersion": ${info.javaVersion?.let { "\"$it\"" } ?: "null"},""")
        appendLine("""  "overallType": "${info.overallType}",""")
        appendLine("""  "isAndroidProject": ${info.isAndroidProject},""")
        appendLine("""  "isKmpProject": ${info.isKmpProject},""")
        info.compileSdk?.let { appendLine("""  "compileSdk": "$it",""") }
        info.minSdk?.let { appendLine("""  "minSdk": "$it",""") }
        info.targetSdk?.let { appendLine("""  "targetSdk": "$it",""") }
        appendLine("""  "moduleCount": ${info.modules.size},""")
        appendLine("""  "modules": [""")

        info.modules.forEachIndexed { index, mod ->
            val comma = if (index < info.modules.size - 1) "," else ""
            appendLine("""    {""")
            appendLine("""      "name": "${mod.name}",""")
            appendLine("""      "type": "${mod.type.displayName}",""")
            appendLine("""      "hasAndroidManifest": ${mod.hasAndroidManifest},""")
            appendLine("""      "plugins": [${mod.plugins.joinToString(", ") { "\"$it\"" }}],""")
            appendLine("""      "dependencies": [${mod.dependencies.joinToString(", ") { "\"$it\"" }}],""")
            appendLine("""      "sourceSets": [${mod.sourceSetDirs.joinToString(", ") { "\"$it\"" }}]""")
            appendLine("""    }$comma""")
        }

        appendLine("  ]")
        appendLine("}")
    }
}
