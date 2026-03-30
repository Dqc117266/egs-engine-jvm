package com.dqc.egsengine.feature.scaffold.data.ddl.model

data class ColumnSchema(
    val name: String,
    val sqlType: String,
    val kotlinType: String,
    val nullable: Boolean,
    val isPrimaryKey: Boolean,
    val isAutoIncrement: Boolean,
    val defaultValue: String?,
    val comment: String?,
    val length: Int? = null,
)
