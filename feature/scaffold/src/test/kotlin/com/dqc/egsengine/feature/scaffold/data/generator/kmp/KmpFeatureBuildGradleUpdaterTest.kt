package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpFeatureBuildGradleUpdaterTest {
    private val updater = KmpFeatureBuildGradleUpdater()

    @Test
    fun `ensureKtorfitPlugins is idempotent`() {
        val with =
            """
plugins {
    alias(libs.plugins.cmp.feature.convention)

    alias(libs.plugins.ktrofit)
    alias(libs.plugins.kmp.ktorfit.ksp.convention)
}
            """.trimIndent()
        assertEquals(with, updater.ensureKtorfitPlugins(with))
    }

    @Test
    fun `ensureKtorfitPlugins inserts before closing plugins brace`() {
        val before =
            """
plugins {
    alias(libs.plugins.cmp.feature.convention)
}
            """.trimIndent()
        val after = updater.ensureKtorfitPlugins(before)
        assertTrue(after.contains("libs.plugins.ktrofit"))
        assertTrue(after.contains("kmp.ktorfit.ksp.convention"))
        assertEquals(after, updater.ensureKtorfitPlugins(after))
    }

    @Test
    fun `replaceCmpConventionWithNoJsAndRoom`() {
        val before =
            """
plugins {
    alias(libs.plugins.cmp.feature.convention)
}
            """.trimIndent()
        val after = updater.replaceCmpConventionWithNoJsAndRoom(before)
        assertTrue(after.contains("cmp.feature.no.js.convention"))
        assertTrue(after.contains("org.convention.kmp.room"))
        assertTrue(!after.contains("alias(libs.plugins.cmp.feature.convention)"))
    }

    @Test
    fun `ensureKotlinBlockWithDependencies adds network`() {
        val before =
            """
plugins {
    alias(libs.plugins.x)
}
            """.trimIndent()
        val after =
            updater.ensureKotlinBlockWithDependencies(
                before,
                gradleProjectRef = "projects.coreBase.network",
                depLine = "implementation(projects.coreBase.network)",
            )
        assertTrue(after.contains("kotlin {"))
        assertTrue(after.contains("projects.coreBase.network"))
        assertEquals(
            after,
            updater.ensureKotlinBlockWithDependencies(
                after,
                gradleProjectRef = "projects.coreBase.network",
                depLine = "implementation(projects.coreBase.network)",
            ),
        )
    }
}
