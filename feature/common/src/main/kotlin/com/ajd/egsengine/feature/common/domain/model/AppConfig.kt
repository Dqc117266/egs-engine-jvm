package com.ajd.egsengine.feature.common.domain.model

data class AppConfig(
    val apiBaseUrl: String = DEFAULT_API_URL,
    val apiToken: String = "",
    val workDir: String = System.getProperty("user.dir"),
) {
    companion object {
        const val DEFAULT_API_URL = "http://localhost:8000"
    }
}
