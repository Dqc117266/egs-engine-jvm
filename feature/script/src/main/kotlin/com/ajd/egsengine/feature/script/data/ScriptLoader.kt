package com.ajd.egsengine.feature.script.data

import com.ajd.egsengine.feature.script.domain.model.Script
import org.slf4j.LoggerFactory
import java.io.File

class ScriptLoader {
    private val logger = LoggerFactory.getLogger(ScriptLoader::class.java)

    fun loadFromFile(file: File): Script? {
        if (!file.exists()) {
            logger.error("Script file not found: ${file.absolutePath}")
            return null
        }

        return try {
            val lines = file.readLines()
            parseScript(file.nameWithoutExtension, lines)
        } catch (e: Exception) {
            logger.error("Failed to load script: ${file.absolutePath}", e)
            null
        }
    }

    fun loadFromDirectory(directory: File): List<Script> {
        if (!directory.isDirectory) {
            logger.error("Not a directory: ${directory.absolutePath}")
            return emptyList()
        }

        return directory.listFiles { _, name -> name.endsWith(".egs") }
            ?.mapNotNull { loadFromFile(it) }
            ?: emptyList()
    }

    private fun parseScript(name: String, lines: List<String>): Script {
        val commands = lines
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        return Script(
            name = name,
            commands = commands,
            sourcePath = name,
        )
    }
}
