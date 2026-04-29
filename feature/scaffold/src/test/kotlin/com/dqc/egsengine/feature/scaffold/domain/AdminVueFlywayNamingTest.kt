/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

internal class AdminVueFlywayNamingTest {

    @Test
    fun `reuses migration file for same module slug`() {
        val d = Files.createTempDirectory("flyway").toFile()
        File(d, "V13__sys_menu_food.sql").writeText("-- x")
        assertEquals("V13__sys_menu_food.sql", resolveSysMenuMigrationFileName(d, "food"))
    }

    @Test
    fun `allocates next global version when no file for module yet`() {
        val d = Files.createTempDirectory("flyway2").toFile()
        File(d, "V11__other.sql").writeText("")
        assertEquals("V12__sys_menu_bar.sql", resolveSysMenuMigrationFileName(d, "bar"))
    }

    @Test
    fun `fresh directory starts at V1`() {
        val d = Files.createTempDirectory("flyway3").toFile()
        assertEquals("V1__sys_menu_baz.sql", resolveSysMenuMigrationFileName(d, "baz"))
    }
}
