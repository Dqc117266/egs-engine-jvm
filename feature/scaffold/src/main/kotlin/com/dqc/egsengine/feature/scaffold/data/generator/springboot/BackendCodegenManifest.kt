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
    /** Form control type for admin Vue codegen. */
    val formControl: FormControl = FormControl.INPUT,
) {
    /** FreeMarker: prefer `col.pk` over `col.isPk` (JavaBean ambiguity). */
    val pk: Boolean get() = isPk
}

/**
 * Form control types for admin Vue codegen.
 * Determines which Element Plus component to render for each column.
 */
enum class FormControl {
    INPUT,
    TEXTAREA,
    NUMBER,
    SWITCH,
    IMAGE_UPLOAD,
    ;

    companion object {
        /**
         * Infer form control from column metadata.
         * Rules:
         * - Field name matches image/avatar/icon/photo patterns → IMAGE_UPLOAD
         * - Field name contains description/instruction → TEXTAREA
         * - Int type → NUMBER
         * - Boolean type → SWITCH
         * - Otherwise → INPUT
         */
        fun infer(kotlinName: String, kotlinType: String, tsType: String): FormControl {
            val name = kotlinName.lowercase()
            return when {
                // Image fields
                name.endsWith("imageurl") || name.endsWith("image") ||
                name.endsWith("avatar") || name.endsWith("icon") ||
                name.endsWith("photourl") || name.endsWith("picurl") ||
                name.endsWith("coverurl") || name.endsWith("cover") ||
                (name.contains("image") && name.endsWith("url")) ||
                (name.contains("photo") && name.endsWith("url")) ||
                (name.contains("pic") && name.endsWith("url"))
                    -> IMAGE_UPLOAD

                // Textarea fields
                name.contains("description") || name.contains("instruction") ||
                name.contains("remark") || name.contains("content") ||
                name.contains("note") || name.contains("comment")
                    -> TEXTAREA

                // Number fields
                tsType == "number" && kotlinType == "Int" -> NUMBER

                // Boolean fields
                tsType == "boolean" -> SWITCH

                else -> INPUT
            }
        }
    }
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
