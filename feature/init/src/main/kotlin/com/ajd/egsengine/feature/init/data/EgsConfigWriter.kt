package com.ajd.egsengine.feature.init.data

import com.ajd.egsengine.feature.init.domain.model.EgsConfig
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

class EgsConfigWriter {
    private val logger = LoggerFactory.getLogger(EgsConfigWriter::class.java)

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun write(config: EgsConfig, projectRoot: File) {
        val egsDir = projectRoot.resolve(".egs")
        egsDir.mkdirs()

        val configFile = egsDir.resolve("config.json")
        val content = json.encodeToString(EgsConfig.serializer(), config)
        configFile.writeText(content)

        logger.info("Wrote config to: ${configFile.absolutePath}")
    }
}
