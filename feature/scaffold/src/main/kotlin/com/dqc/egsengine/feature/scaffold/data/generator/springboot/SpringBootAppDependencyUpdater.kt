package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import org.slf4j.LoggerFactory
import java.io.File

class SpringBootAppDependencyUpdater {
    private val logger = LoggerFactory.getLogger(SpringBootAppDependencyUpdater::class.java)

    fun ensureFeatureDependency(
        backendRoot: File,
        moduleName: String,
    ): Boolean {
        val buildFile = backendRoot.resolve("app/build.gradle.kts")
        if (!buildFile.isFile) {
            logger.debug("No backend app build.gradle.kts at {}", buildFile.path)
            return false
        }

        val dependencyLine = "    implementation(project(\":feature:$moduleName\"))"
        val original = buildFile.readText()
        if (original.contains("project(\":feature:$moduleName\")")) {
            return false
        }

        val updated = insertDependency(original, dependencyLine)
        if (updated == original) {
            return false
        }

        buildFile.writeText(updated)
        logger.info("Added :feature:{} dependency to {}", moduleName, buildFile.path)
        return true
    }

    internal fun insertDependency(
        text: String,
        dependencyLine: String,
    ): String {
        val dependenciesBlockPattern = Regex("""dependencies\s*\{([\s\S]*?)\}""")
        val match = dependenciesBlockPattern.find(text) ?: return text
        val body = match.groupValues[1]
        if (body.contains(dependencyLine.trim())) {
            return text
        }

        val featureDependencyRegex = Regex("""(?m)^\s*implementation\(project\(\":feature:[^\"]+\"\)\)\s*$""")
        val featureMatches = featureDependencyRegex.findAll(body).toList()
        val newBody =
            if (featureMatches.isNotEmpty()) {
                val last = featureMatches.last()
                body.replaceRange(last.range.last + 1, last.range.last + 1, "\n$dependencyLine")
            } else {
                val insertionPoint = body.indexOfFirstNonBlankLineAfterProjectDependencies()
                if (insertionPoint >= 0) {
                    body.replaceRange(insertionPoint, insertionPoint, "$dependencyLine\n")
                } else {
                    body.trimEnd() + if (body.isBlank()) "\n$dependencyLine\n" else "\n$dependencyLine\n"
                }
            }

        return text.replaceRange(match.groups[1]!!.range, newBody)
    }

    private fun String.indexOfFirstNonBlankLineAfterProjectDependencies(): Int {
        val lines = lines()
        var offset = 0
        var lastProjectDependencyOffset = -1
        for (line in lines) {
            val trimmed = line.trim()
            val isProjectDependency = trimmed.startsWith("implementation(project(")
            if (isProjectDependency) {
                lastProjectDependencyOffset = offset + line.length + 1
            }
            offset += line.length + 1
        }
        return lastProjectDependencyOffset.takeIf { it >= 0 } ?: 0
    }
}
