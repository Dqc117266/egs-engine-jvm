package com.ajd.egsengine.feature.scaffold.data

import com.ajd.egsengine.feature.init.domain.model.EgsConfig
import kotlinx.serialization.json.Json
import java.io.File

class EgsConfigReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(projectRoot: File): EgsConfig {
        val configFile = projectRoot.resolve(".egs/config.json")
        require(configFile.exists()) {
            "No .egs/config.json found at ${projectRoot.absolutePath}. Run 'egs init' first."
        }
        return json.decodeFromString(EgsConfig.serializer(), configFile.readText())
    }
}
