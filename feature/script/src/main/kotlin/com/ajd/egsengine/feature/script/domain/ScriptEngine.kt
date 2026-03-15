package com.ajd.egsengine.feature.script.domain

import com.ajd.egsengine.feature.base.command.CommandResult
import com.ajd.egsengine.feature.script.data.ScriptLoader
import com.ajd.egsengine.feature.script.domain.model.Script
import org.slf4j.LoggerFactory
import java.io.File

class ScriptEngine(
    private val scriptLoader: ScriptLoader,
) {
    private val logger = LoggerFactory.getLogger(ScriptEngine::class.java)

    fun loadScript(path: String): Script? {
        val file = File(path)
        return scriptLoader.loadFromFile(file)
    }

    fun listScripts(directory: String): List<Script> {
        val dir = File(directory)
        return scriptLoader.loadFromDirectory(dir)
    }

    fun validateScript(script: Script): List<String> {
        val errors = mutableListOf<String>()

        if (script.name.isBlank()) {
            errors.add("Script name is empty")
        }

        if (script.commands.isEmpty()) {
            errors.add("Script has no commands")
        }

        return errors
    }
}
