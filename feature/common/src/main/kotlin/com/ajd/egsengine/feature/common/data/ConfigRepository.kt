package com.ajd.egsengine.feature.common.data

import com.ajd.egsengine.feature.common.domain.model.AppConfig
import java.io.File
import java.util.Properties

interface ConfigRepository {
    fun loadConfig(): AppConfig
    fun saveConfig(config: AppConfig)
}

class ConfigRepositoryImpl : ConfigRepository {
    private val configFile: File by lazy {
        val home = System.getProperty("user.home")
        File(home, ".egs-engine/config.properties").also {
            it.parentFile?.mkdirs()
        }
    }

    override fun loadConfig(): AppConfig {
        if (!configFile.exists()) return AppConfig()

        val props = Properties()
        configFile.inputStream().use { props.load(it) }

        return AppConfig(
            apiBaseUrl = props.getProperty("api.base.url", AppConfig.DEFAULT_API_URL),
            apiToken = props.getProperty("api.token", ""),
            workDir = props.getProperty("work.dir", System.getProperty("user.dir")),
        )
    }

    override fun saveConfig(config: AppConfig) {
        val props = Properties()
        props.setProperty("api.base.url", config.apiBaseUrl)
        props.setProperty("api.token", config.apiToken)
        props.setProperty("work.dir", config.workDir)

        configFile.outputStream().use { props.store(it, "egs-engine configuration") }
    }
}
