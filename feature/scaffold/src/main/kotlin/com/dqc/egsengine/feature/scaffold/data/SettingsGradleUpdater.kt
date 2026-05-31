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
        // Try to find an existing include(":feature:...") line and append after the last one
        val featurePattern = Regex("""include\(":feature:[^"]+"\)""")
        val lastFeatureMatch = featurePattern.findAll(content).lastOrNull()

        if (lastFeatureMatch != null) {
            val insertPos = lastFeatureMatch.range.last + 1
            return buildString {
                append(content.substring(0, insertPos))
                append("\ninclude(\"$modulePath\")")
                append(content.substring(insertPos))
            }
        }

        // Fallback: look for any include("...") block pattern (multi-line include(...))
        val includeBlockPattern = Regex(
            """(include\s*\()([^)]*?)(\))""",
            RegexOption.DOT_MATCHES_ALL,
        )

        val blockMatch = includeBlockPattern.find(content)
        if (blockMatch != null) {
            val existingEntries = blockMatch.groupValues[2]
            val lastEntry = existingEntries.trimEnd()

            val newEntry = if (lastEntry.endsWith(",")) {
                "$lastEntry\n    \"$modulePath\","
            } else {
                "$lastEntry,\n    \"$modulePath\","
            }

            return content.replaceRange(
                blockMatch.groups[2]!!.range,
                newEntry,
            )
        }

        // Final fallback: append at end
        return "$content\ninclude(\"$modulePath\")\n"
    }
}
