/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot

/**
 * Serialized under `.egs-generated.json` as `"codegen": { ... }` for admin (and tooling) to stay in sync
 * with [com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootCodegenModelBuilder].
 */
data class BackendCodegenManifest(
    val schemaVersion: Int = 1,
    val entityPascal: String,
    val entityCamel: String,
    val restPath: String,
    val tableSqlName: String,
    val backendModuleName: String,
    val basePackage: String,
    val pkField: String,
    val pkTsType: String,
    val columns: List<BackendCodegenManifestColumn>,
)

data class BackendCodegenManifestColumn(
    val kotlinName: String,
    val kotlinType: String,
    val tsType: String,
    val nullable: Boolean,
    val isPk: Boolean,
    /** Included in create or update form (excludes PK and pure audit columns). */
    val inBusinessForm: Boolean,
) {
    /** FreeMarker: prefer `col.pk` over `col.isPk` (JavaBean ambiguity). */
    val pk: Boolean get() = isPk
}

object KotlinToTsTypeMapper {
    fun toTsType(kotlinType: String): String =
        when (kotlinType) {
            "Long", "Int" -> "number"
            "String" -> "string"
            "Boolean" -> "boolean"
            "Double", "Float" -> "number"
            "Instant" -> "string"
            else -> "unknown"
        }
}
