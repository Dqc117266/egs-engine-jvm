package com.ajd.egsengine.feature.script.domain.model

data class Script(
    val name: String,
    val commands: List<String>,
    val sourcePath: String = "",
    val description: String = "",
)
