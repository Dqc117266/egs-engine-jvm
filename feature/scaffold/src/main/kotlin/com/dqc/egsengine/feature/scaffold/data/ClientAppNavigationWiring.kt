/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data

import org.slf4j.LoggerFactory
import java.io.File

/**
 * After [FeatureDiUpdater], wires the new screen into the client `app` module:
 * `NavigationRoute`, `MainTemplateScreen` NavHost, `app/build.gradle.kts`, and `App.kt` Koin modules.
 */
class ClientAppNavigationWiring {
    private val logger = LoggerFactory.getLogger(ClientAppNavigationWiring::class.java)

    fun wireIfPossible(
        clientRoot: File,
        moduleName: String,
        modulePackage: String,
        pageName: String,
        skipNav: Boolean,
        skipAppWire: Boolean,
    ) {
        val appDir = clientRoot.resolve("app")
        if (!appDir.isDirectory) {
            logger.debug("No app/ under client root; skip app wiring")
            return
        }
        val navFile = findFile(appDir, "NavigationRoute.kt")
        val mainFile = findFile(appDir, "MainTemplateScreen.kt")
        val appKt = findFile(appDir, "App.kt")
        val gradle = appDir.resolve("build.gradle.kts")
        if (navFile == null || mainFile == null) {
            logger.debug("NavigationRoute.kt or MainTemplateScreen.kt not found under app/; skip")
            return
        }

        val pascal = pageName.replaceFirstChar { it.uppercase() }
        val camel = pageName.replaceFirstChar { it.lowercase() }
        val routeName = pascal
        val screenFqn = "$modulePackage.presentation.screen.$camel.${pascal}Screen"
        val appPkg = inferAppPackage(appKt, mainFile, navFile)
        val featureModulesName = featureModulesBindingName(moduleName)

        if (!skipNav) {
            appendNavigationRoute(navFile, routeName)
            appendMainTemplateComposable(mainFile, appPkg, routeName, screenFqn, pascal)
        } else {
            logger.info("Skipping NavigationRoute / MainTemplateScreen (--skip-nav)")
        }

        if (!skipAppWire) {
            if (gradle.exists()) {
                appendGradleFeatureDependency(gradle, moduleName)
            }
            if (appKt != null) {
                appendKoinModules(appKt, appPkg, moduleName, featureModulesName)
            }
        } else {
            logger.info("Skipping app Gradle / Koin (--skip-app-wire)")
        }
    }

    private fun findFile(appDir: File, simpleName: String): File? =
        appDir.walkTopDown().maxDepth(25).firstOrNull { it.isFile && it.name == simpleName }

    private fun inferAppPackage(appKt: File?, mainFile: File, navFile: File): String {
        if (appKt != null) {
            val pkg = readPackage(appKt)
            if (pkg != null) return pkg
        }
        return readPackage(mainFile) ?: readPackage(navFile) ?: "com.example.app"
    }

    private fun readPackage(file: File): String? {
        val line = file.readLines().firstOrNull { it.trim().startsWith("package ") } ?: return null
        return line.trim().removePrefix("package ").trim()
    }

    private fun appendNavigationRoute(file: File, routeName: String) {
        var text = file.readText()
        val marker = "data object $routeName : NavigationRoute"
        if (text.contains(marker)) {
            logger.debug("Navigation route $routeName already present")
            return
        }
        val insert = """

    @Serializable
    data object $routeName : NavigationRoute
"""
        if (!text.contains("sealed interface NavigationRoute")) return
        val closeBrace = text.lastIndexOf('}')
        if (closeBrace < 0) return
        text = text.substring(0, closeBrace) + insert + "\n" + text.substring(closeBrace)
        file.writeText(text)
        logger.info("Appended NavigationRoute.$routeName to ${file.name}")
    }

    private fun appendMainTemplateComposable(
        mainFile: File,
        appPackage: String,
        routeName: String,
        screenImportFqn: String,
        pascalScreen: String,
    ) {
        var text = mainFile.readText()
        val composableMarker = "composable<NavigationRoute.$routeName>"
        if (text.contains(composableMarker)) {
            logger.debug("MainTemplateScreen already has $routeName")
            return
        }
        val importLine = "import $screenImportFqn"
        if (!text.contains(importLine)) {
            val pkgLine = text.lineSequence().indexOfFirst { it.trim().startsWith("package ") }
            val insertAt = if (pkgLine >= 0) {
                text.lines().take(pkgLine + 1).joinToString("\n") + "\n" + importLine + "\n" +
                    text.lines().drop(pkgLine + 1).joinToString("\n")
            } else {
                importLine + "\n\n" + text
            }
            text = insertAt
        }
        val graphBlock = Regex("""navController\.createGraph\([^)]*\)\s*\{""").find(text)
        if (graphBlock == null) {
            logger.warn("Could not find createGraph { } in MainTemplateScreen; add composable manually")
            return
        }
        val blockStart = graphBlock.range.last + 1
        val insertComposable = """
            composable<NavigationRoute.$routeName> {
                ${pascalScreen}Screen()
            }
"""
        text = text.substring(0, blockStart) + "\n" + insertComposable + text.substring(blockStart)
        mainFile.writeText(text)
        logger.info("Appended composable for $routeName to MainTemplateScreen")
    }

    private fun appendGradleFeatureDependency(gradle: File, moduleName: String) {
        var text = gradle.readText()
        val dep = "implementation(projects.feature.$moduleName)"
        if (text.contains("projects.feature.$moduleName")) {
            return
        }
        val depsIdx = text.indexOf("dependencies {")
        if (depsIdx < 0) return
        val insertPoint = depsIdx + "dependencies {".length
        val toInsert = "\n    $dep\n"
        text = text.substring(0, insertPoint) + toInsert + text.substring(insertPoint)
        gradle.writeText(text)
        logger.info("Added Gradle dependency $dep")
    }

    private fun appendKoinModules(appKt: File, appPackage: String, moduleName: String, featureModulesName: String) {
        var text = appKt.readText()
        if (text.contains("modules($featureModulesName)")) {
            logger.debug("Koin already lists $featureModulesName")
            return
        }
        val importLine = "import $appPackage.feature.$moduleName.$featureModulesName"
        if (!text.contains(importLine)) {
            val pkgLine = text.lines().indexOfFirst { it.trim().startsWith("package ") }
            if (pkgLine >= 0) {
                val head = text.lines().take(pkgLine + 1).joinToString("\n")
                val tail = text.lines().drop(pkgLine + 1).joinToString("\n")
                text = "$head\n$importLine\n$tail"
            }
        }
        val modulesLine = "            modules($featureModulesName)"
        val featureModulesRegex = Regex("""modules\(feature\w+Modules\)""")
        val lastFeature = featureModulesRegex.findAll(text).lastOrNull()
        val insertPos: Int = if (lastFeature != null) {
            lastFeature.range.last + 1
        } else {
            val m = Regex("""modules\(appModule\)""").find(text)
            when {
                m != null -> m.range.last + 1
                else -> {
                    val i = text.indexOf("androidContext(")
                    if (i < 0) {
                        logger.warn("Could not find insertion point for Koin modules in App.kt")
                        return
                    }
                    i
                }
            }
        }
        text = text.substring(0, insertPos) + "\n$modulesLine" + text.substring(insertPos)
        appKt.writeText(text)
        logger.info("Appended Koin $featureModulesName to App.kt")
    }
}

internal fun featureModulesBindingName(moduleName: String): String =
    "feature" + moduleName.replaceFirstChar { it.uppercaseChar() } + "Modules"
