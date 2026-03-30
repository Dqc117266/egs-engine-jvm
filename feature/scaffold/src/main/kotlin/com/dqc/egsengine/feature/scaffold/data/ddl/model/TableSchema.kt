package com.dqc.egsengine.feature.scaffold.data.ddl.model

data class TableSchema(
    val tableName: String,
    val columns: List<ColumnSchema>,
    val primaryKey: String?,
    val indexes: List<IndexSchema> = emptyList(),
    val comment: String? = null,
)
