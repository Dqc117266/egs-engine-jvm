/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import org.slf4j.LoggerFactory
import java.io.File

/**
 * Idempotent patches for Android feature [build.gradle.kts]: adds
 * [coreBase.preferences] / [coreBase.database] project dependencies when missing
 * (matches [com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpFeatureBuildGradleUpdater]).
 */
class AndroidFeatureBuildGradleUpdater {

    private val logger = LoggerFactory.getLogger(AndroidFeatureBuildGradleUpdater::class.java)

    fun applyAfterDatabaseGen(subProjectRoot: File, moduleName: String) {
        val file = moduleBuildFile(subProjectRoot, moduleName) ?: return
        var text = file.readText()
        val original = text
        val gradleProjectRef = "projects.coreBase.database"
        val depLine = "implementation(projects.coreBase.database)"
        if (text.contains(gradleProjectRef)) return

        text = insertIntoDependenciesBlock(text, gradleProjectRef, depLine)
            ?: appendDependenciesBlock(text, depLine)

        if (text != original) {
            file.writeText(text)
            logger.info("Updated {} for coreBase.database", file.path)
        }
    }

    fun applyAfterPrefsGen(subProjectRoot: File, moduleName: String) {
        val file = moduleBuildFile(subProjectRoot, moduleName) ?: return
        var text = file.readText()
        val original = text
        val gradleProjectRef = "projects.coreBase.preferences"
        val depLine = "implementation(projects.coreBase.preferences)"
        if (text.contains(gradleProjectRef)) return

        text = insertIntoDependenciesBlock(text, gradleProjectRef, depLine)
            ?: appendDependenciesBlock(text, depLine)

        if (text != original) {
            file.writeText(text)
            logger.info("Updated {} for coreBase.preferences", file.path)
        }
    }

    private fun moduleBuildFile(subProjectRoot: File, moduleName: String): File? {
        val f = subProjectRoot.resolve("feature/$moduleName/build.gradle.kts")
        if (!f.isFile) {
            logger.debug("No feature build file at {}", f.path)
            return null
        }
        return f
    }

    private fun insertIntoDependenciesBlock(text: String, gradleProjectRef: String, depLine: String): String? {
        if (text.contains(gradleProjectRef)) return text
        val marker = "dependencies {"
        val idx = text.indexOf(marker)
        if (idx < 0) return null
        val openBrace = idx + marker.length - 1
        var depth = 0
        var i = openBrace
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val inner = text.substring(openBrace + 1, i)
                        if (inner.contains(gradleProjectRef)) return text
                        val insert = "\n    $depLine"
                        return text.substring(0, i) + insert + text.substring(i)
                    }
                }
            }
            i++
        }
        return null
    }

    private fun appendDependenciesBlock(text: String, depLine: String): String =
        text.trimEnd() + """

dependencies {
    $depLine
}
"""
}
