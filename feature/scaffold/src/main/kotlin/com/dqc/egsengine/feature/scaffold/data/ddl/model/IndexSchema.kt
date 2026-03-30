package com.dqc.egsengine.feature.scaffold.data.ddl.model

data class IndexSchema(
    val name: String,
    val columns: List<String>,
    val unique: Boolean,
)
