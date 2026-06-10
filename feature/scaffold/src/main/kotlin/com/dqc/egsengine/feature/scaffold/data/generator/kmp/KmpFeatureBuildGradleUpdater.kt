/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import org.slf4j.LoggerFactory
import java.io.File

/**
 * Idempotent patches for KMP feature [build.gradle.kts]: Ktorfit plugins + coreBase.network after API sync;
 * Room / no-js convention + coreBase.database after database codegen.
 */
class KmpFeatureBuildGradleUpdater {
    private val logger = LoggerFactory.getLogger(KmpFeatureBuildGradleUpdater::class.java)

    fun applyAfterApiSync(
        subProjectRoot: File,
        moduleName: String,
    ) {
        val file = moduleBuildFile(subProjectRoot, moduleName) ?: return
        var text = file.readText()
        val original = text

        text = ensureKtorfitPlugins(text)
        text =
            ensureKotlinBlockWithDependencies(
                text,
                gradleProjectRef = "projects.coreBase.network",
                depLine = "implementation(projects.coreBase.network)",
            )

        if (text != original) {
            file.writeText(text)
            logger.info("Updated {} for Ktorfit / coreBase.network", file.path)
        }
    }

    fun applyAfterPrefsGen(
        subProjectRoot: File,
        moduleName: String,
    ) {
        val file = moduleBuildFile(subProjectRoot, moduleName) ?: return
        var text = file.readText()
        val original = text
        text =
            ensureKotlinBlockWithDependencies(
                text,
                gradleProjectRef = "projects.coreBase.preferences",
                depLine = "implementation(projects.coreBase.preferences)",
            )
        if (text != original) {
            file.writeText(text)
            logger.info("Updated {} for coreBase.preferences", file.path)
        }
    }

    fun applyAfterDatabaseGen(
        subProjectRoot: File,
        moduleName: String,
    ) {
        val file = moduleBuildFile(subProjectRoot, moduleName) ?: return
        var text = file.readText()
        val original = text

        text = replaceCmpConventionWithNoJsAndRoom(text)
        text = ensureKtorfitPlugins(text)
        text =
            ensureKotlinBlockWithDependencies(
                text,
                gradleProjectRef = "projects.coreBase.network",
                depLine = "implementation(projects.coreBase.network)",
            )
        text =
            ensureKotlinBlockWithDependencies(
                text,
                gradleProjectRef = "projects.coreBase.database",
                depLine = "implementation(projects.coreBase.database)",
            )

        if (text != original) {
            file.writeText(text)
            logger.info("Updated {} for Room / coreBase.database", file.path)
        }
    }

    private fun moduleBuildFile(
        subProjectRoot: File,
        moduleName: String,
    ): File? {
        val f = subProjectRoot.resolve("feature/$moduleName/build.gradle.kts")
        if (!f.isFile) {
            logger.debug("No feature build file at {}", f.path)
            return null
        }
        return f
    }

    internal fun ensureKtorfitPlugins(text: String): String {
        if (text.contains("libs.plugins.ktrofit")) return text
        val anchor = "plugins {"
        val idx = text.indexOf(anchor)
        if (idx < 0) return text
        val insertAt = text.indexOf('}', idx + anchor.length)
        if (insertAt < 0) return text
        val insertion = """

    alias(libs.plugins.ktrofit)
    alias(libs.plugins.kmp.ktorfit.ksp.convention)"""
        return text.substring(0, insertAt) + insertion + text.substring(insertAt)
    }

    internal fun ensureKotlinBlockWithDependencies(
        text: String,
        gradleProjectRef: String,
        depLine: String,
    ): String {
        if (text.contains(gradleProjectRef)) return text

        val indentedDep = "            $depLine"
        val inserted = insertIntoCommonMainDependencies(text, gradleProjectRef, depLine)
        if (inserted != null) return inserted

        val kotlinBlock = """

kotlin {
    sourceSets {
        commonMain.dependencies {
$indentedDep
        }
    }
}"""
        return text.trimEnd() + kotlinBlock + "\n"
    }

    private fun insertIntoCommonMainDependencies(
        text: String,
        gradleProjectRef: String,
        depLine: String,
    ): String? {
        val marker = "commonMain.dependencies {"
        val idx = text.indexOf(marker)
        if (idx < 0) return null
        val afterBrace = idx + marker.length
        val closeIdx = text.indexOf('}', afterBrace)
        if (closeIdx < 0) return null
        val inner = text.substring(afterBrace, closeIdx)
        if (inner.contains(gradleProjectRef)) return text
        val insert = "\n            $depLine"
        return text.substring(0, closeIdx) + insert + "\n        " + text.substring(closeIdx)
    }

    internal fun replaceCmpConventionWithNoJsAndRoom(text: String): String {
        var t = text
        val featureConventionLines =
            listOf(
                "    alias(libs.plugins.cmp.feature.ui.convention)",
                "    alias(libs.plugins.cmp.feature.convention)",
            )
        val replacement = """    alias(libs.plugins.cmp.feature.no.js.convention)
    alias(libs.plugins.mifos.kmp.room)"""
        val matchedLine = featureConventionLines.firstOrNull { t.contains(it) }
        if (matchedLine != null) {
            t = t.replace(matchedLine, replacement)
        } else if (!t.contains("libs.plugins.mifos.kmp.room") && t.contains("cmp.feature.no.js.convention")) {
            val search = "alias(libs.plugins.cmp.feature.no.js.convention)"
            val i = t.indexOf(search)
            if (i >= 0) {
                val lineEnd = t.indexOf('\n', i)
                if (lineEnd > i) {
                    t = t.substring(0, lineEnd + 1) + "    alias(libs.plugins.mifos.kmp.room)\n" + t.substring(lineEnd + 1)
                }
            }
        }
        return t
    }
}
