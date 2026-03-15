package com.dqc.egsengine.feature.scaffold.data

import org.slf4j.LoggerFactory
import java.io.File

class SettingsGradleUpdater {
    private val logger = LoggerFactory.getLogger(SettingsGradleUpdater::class.java)

    fun update(projectRoot: File, moduleName: String) {
        val settingsFile = projectRoot.resolve("settings.gradle.kts").takeIf { it.exists() }
            ?: projectRoot.resolve("settings.gradle").takeIf { it.exists() }
            ?: throw IllegalStateException("No settings.gradle found at ${projectRoot.absolutePath}")

        val content = settingsFile.readText()
        val modulePath = ":feature:$moduleName"

        if (content.contains("\"$modulePath\"")) {
            logger.info("Module $modulePath already in settings.gradle")
            return
        }

        val updated = insertModule(content, modulePath)
        settingsFile.writeText(updated)
        logger.info("Added $modulePath to ${settingsFile.name}")
    }

    private fun insertModule(content: String, modulePath: String): String {
        val includeBlockPattern = Regex(
            """(include\s*\()([^)]*?)(\))""",
            RegexOption.DOT_MATCHES_ALL,
        )

        val match = includeBlockPattern.find(content) ?: return appendInclude(content, modulePath)

        val existingEntries = match.groupValues[2]
        val lastEntry = existingEntries.trimEnd()

        val newEntry = if (lastEntry.endsWith(",")) {
            "$lastEntry\n    \"$modulePath\","
        } else {
            "$lastEntry,\n    \"$modulePath\","
        }

        return content.replaceRange(
            match.groups[2]!!.range,
            newEntry,
        )
    }

    private fun appendInclude(content: String, modulePath: String): String =
        "$content\ninclude(\"$modulePath\")\n"
}
