/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot.database

import com.dqc.egsengine.feature.scaffold.data.ddl.model.ColumnSchema
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema

data class SchemaTraits(
    /** `created_at` / `updated_at` present and enabled by options → JPA extends [BaseEntity], domain carries instants. */
    val hasAuditDbColumns: Boolean,
    val useJpaAuditingBase: Boolean,
    /** `status` integer column (demo-style), not a Java enum in v1. */
    val hasStatusColumn: Boolean,
    val hasSoftDeleteColumn: Boolean,
    val softDeleteColumnName: String?,
)

class SchemaTraitInferrer {
    fun infer(
        table: TableSchema,
        options: SpringBootOpinionatedOptions,
    ): SchemaTraits {
        val cols = table.columns.map { it.name.lowercase() }.toSet()
        val hasCreated = "created_at" in cols
        val hasUpdated = "updated_at" in cols
        val hasAuditDb = hasCreated && hasUpdated
        val useBase = options.auditColumns && hasAuditDb

        val hasStatus =
            table.columns.any {
                it.name.equals("status", ignoreCase = true) &&
                    it.kotlinType == "Int"
            }

        val softName = listOf("deleted_at", "is_deleted").firstOrNull { it in cols }
        val hasSoft = options.softDelete && softName != null

        return SchemaTraits(
            hasAuditDbColumns = hasAuditDb,
            useJpaAuditingBase = useBase,
            hasStatusColumn = hasStatus && options.statusEnum,
            hasSoftDeleteColumn = hasSoft,
            softDeleteColumnName = if (hasSoft) softName else null,
        )
    }

    fun columnByName(
        table: TableSchema,
        name: String,
    ): ColumnSchema? = table.columns.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
