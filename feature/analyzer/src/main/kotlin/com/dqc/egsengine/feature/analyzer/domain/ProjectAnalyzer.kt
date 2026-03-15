package com.dqc.egsengine.feature.analyzer.domain

import com.dqc.egsengine.feature.analyzer.data.GradleProjectScanner
import com.dqc.egsengine.feature.analyzer.domain.model.ProjectInfo
import com.dqc.egsengine.feature.analyzer.domain.model.ProjectType
import org.slf4j.LoggerFactory
import java.io.File

class ProjectAnalyzer(
    private val scanner: GradleProjectScanner,
) {
    private val logger = LoggerFactory.getLogger(ProjectAnalyzer::class.java)

    fun analyze(projectPath: String): ProjectInfo {
        val dir = File(projectPath).let {
            if (it.isAbsolute) it else it.absoluteFile
        }

        require(dir.isDirectory) { "Path is not a directory: $dir" }
        logger.info("Analyzing project at: ${dir.absolutePath}")

        return scanner.scanProject(dir)
    }

    fun generateSummary(info: ProjectInfo): String = buildString {
        appendLine()
        appendLine("=" .repeat(60))
        appendLine("  Project Analysis: ${info.name}")
        appendLine("=".repeat(60))
        appendLine()

        appendLine("  Type            : ${info.overallType}")
        appendLine("  Root Path       : ${info.rootPath}")
        appendLine("  Gradle Version  : ${info.gradleVersion}")
        info.kotlinVersion?.let { appendLine("  Kotlin Version  : $it") }
        info.javaVersion?.let { appendLine("  Java Version    : $it") }

        if (info.isAndroidProject) {
            appendLine()
            appendLine("  --- Android Configuration ---")
            info.compileSdk?.let { appendLine("  Compile SDK     : $it") }
            info.minSdk?.let { appendLine("  Min SDK         : $it") }
            info.targetSdk?.let { appendLine("  Target SDK      : $it") }
        }

        if (info.isKmpProject) {
            appendLine()
            appendLine("  --- KMP Targets ---")
            val kmpModules = info.modules.filter { it.type == ProjectType.KOTLIN_MULTIPLATFORM }
            for (mod in kmpModules) {
                val targets = mod.sourceSetDirs.filter {
                    it.endsWith("Main") || it.endsWith("Test")
                }
                if (targets.isNotEmpty()) {
                    appendLine("  ${mod.name}: ${targets.joinToString(", ")}")
                }
            }
        }

        appendLine()
        appendLine("  --- Modules (${info.modules.size}) ---")

        val maxNameLen = info.modules.maxOfOrNull { it.name.length } ?: 20
        for (module in info.modules) {
            val paddedName = module.name.padEnd(maxNameLen + 2)
            appendLine("  $paddedName [${module.type.displayName}]")
        }

        val countByType = info.moduleCountByType
        appendLine()
        appendLine("  --- Module Statistics ---")
        for ((type, count) in countByType.entries.sortedByDescending { it.value }) {
            appendLine("  ${type.displayName.padEnd(25)} : $count")
        }
        appendLine("  ${"Total".padEnd(25)} : ${info.modules.size}")

        appendLine()
        appendLine("  --- Dependency Graph ---")
        for (module in info.modules) {
            if (module.dependencies.isNotEmpty()) {
                appendLine("  ${module.name}")
                for (dep in module.dependencies) {
                    appendLine("    └── $dep")
                }
            }
        }

        appendLine()
        appendLine("=".repeat(60))
    }
}
