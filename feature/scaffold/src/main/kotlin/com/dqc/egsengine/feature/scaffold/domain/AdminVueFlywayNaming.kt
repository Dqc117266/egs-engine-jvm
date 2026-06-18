/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import java.io.File

/**
 * Picks Flyway migration file name under `backend/.../db/migration/` for `sys_menu` entries for [moduleSlug].
 *
 * Stable contract: **`V?_ _sys_menu_<module>.sql` never stacks multiple versions per module — if one exists, we overwrite it
 * when re-running codegen (Flyway checksum may then require `repair` on upgraded DBs; dev DBs rebuild often.).
 */
internal fun resolveSysMenuMigrationFileName(
    migrationDir: File,
    moduleSlug: String,
): String {
    val escaped = Regex.escape(moduleSlug)
    val pattern = Regex("^V(\\d+)__sys_menu_$escaped\\.sql$")
    migrationDir
        .takeIf { it.isDirectory }
        ?.listFiles()
        ?.forEach { f ->
            if (pattern.matchEntire(f.name) != null) return f.name
        }
    return "V${maxFlywayMigrationVersionNumber(migrationDir) + 1}__sys_menu_$moduleSlug.sql"
}

internal fun maxFlywayMigrationVersionNumber(migrationDir: File): Int = migrationDir
    .takeIf { it.isDirectory }
    ?.listFiles()
    ?.mapNotNull { f ->
        Regex("""^V(\d+)__""")
            .find(f.name)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }?.maxOrNull()
    ?: 0
