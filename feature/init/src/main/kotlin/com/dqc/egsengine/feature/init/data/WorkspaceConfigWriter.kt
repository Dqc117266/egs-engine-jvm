package com.dqc.egsengine.feature.init.data

import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

class WorkspaceConfigWriter {
    private val logger = LoggerFactory.getLogger(WorkspaceConfigWriter::class.java)

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun write(config: WorkspaceConfig, projectRoot: File) {
        val egsDir = projectRoot.resolve(".egs")
        egsDir.mkdirs()

        val configFile = egsDir.resolve("workspace.json")
        val content = json.encodeToString(WorkspaceConfig.serializer(), config)
        configFile.writeText(content)

        logger.info("Wrote workspace config to: {}", configFile.absolutePath)
    }
}
