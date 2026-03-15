package com.ajd.egsengine.feature.init.domain

import com.ajd.egsengine.feature.init.data.EgsConfigWriter
import com.ajd.egsengine.feature.init.domain.model.EgsConfig
import com.ajd.egsengine.feature.init.domain.model.ModuleStructure
import org.slf4j.LoggerFactory
import java.io.File

class ProjectInitializer(
    private val baseClassScanner: BaseClassScanner,
    private val configWriter: EgsConfigWriter,
) {
    private val logger = LoggerFactory.getLogger(ProjectInitializer::class.java)

    fun initialize(projectPath: File): EgsConfig {
        require(projectPath.isDirectory) { "Path is not a directory: $projectPath" }

        val settingsFile = projectPath.resolve("settings.gradle.kts").takeIf { it.exists() }
            ?: projectPath.resolve("settings.gradle").takeIf { it.exists() }

        requireNotNull(settingsFile) { "Not a Gradle project (no settings.gradle found): $projectPath" }

        logger.info("Initializing .egs for project at: ${projectPath.absolutePath}")

        val projectName = detectProjectName(settingsFile)
        val projectType = detectProjectType(projectPath)
        val conventionPluginId = detectConventionPlugin(projectPath)
        val basePackage = detectBasePackage(projectPath)
        val modules = parseIncludedModules(settingsFile)
        val moduleStructure = analyzeModuleStructure(projectPath, modules)
        val baseClasses = baseClassScanner.scan(projectPath, modules)

        val config = EgsConfig(
            projectName = projectName,
            projectType = projectType,
            rootPath = projectPath.absolutePath,
            conventionPluginId = conventionPluginId,
            basePackage = basePackage,
            moduleStructure = moduleStructure,
            baseClasses = baseClasses,
        )

        configWriter.write(config, projectPath)
        return config
    }

    private fun detectProjectName(settingsFile: File): String {
        val content = settingsFile.readText()
        val pattern = Regex("""rootProject\.name\s*=\s*"([^"]+)"""")
        return pattern.find(content)?.groupValues?.get(1) ?: settingsFile.parentFile.name
    }

    private fun detectProjectType(projectRoot: File): String {
        val buildFile = projectRoot.resolve("build.gradle.kts").takeIf { it.exists() }
            ?: projectRoot.resolve("build.gradle").takeIf { it.exists() }

        val content = buildFile?.readText()?.lowercase() ?: ""

        val hasAndroidPlugin = content.contains("com.android") ||
            projectRoot.resolve("app/src/main/AndroidManifest.xml").exists()

        val hasKmpPlugin = content.contains("multiplatform")

        return when {
            hasKmpPlugin && hasAndroidPlugin -> "KMP_ANDROID"
            hasKmpPlugin -> "KMP"
            hasAndroidPlugin -> "ANDROID"
            else -> "KOTLIN_JVM"
        }
    }

    private fun detectConventionPlugin(projectRoot: File): String? {
        val buildLogicDir = projectRoot.resolve("build-logic")
        if (!buildLogicDir.isDirectory) return null

        val featureModules = projectRoot.resolve("feature")
        if (!featureModules.isDirectory) return null

        val sampleBuildFile = featureModules.listFiles()
            ?.firstOrNull { it.isDirectory }
            ?.let { dir ->
                dir.resolve("build.gradle.kts").takeIf { it.exists() }
                    ?: dir.resolve("build.gradle").takeIf { it.exists() }
            }
            ?: return null

        val content = sampleBuildFile.readText()
        val pluginPattern = Regex("""id\s*\(\s*"([^"]+\.convention\.feature[^"]*)"\s*\)""")
        return pluginPattern.find(content)?.groupValues?.get(1)
    }

    private fun detectBasePackage(projectRoot: File): String? {
        val featureDir = projectRoot.resolve("feature")
        if (!featureDir.isDirectory) return null

        val kotlinDirs = mutableListOf<File>()
        featureDir.walkTopDown().forEach { file ->
            if (file.isDirectory && file.name == "kotlin" && file.parentFile.name == "main") {
                kotlinDirs.add(file)
            }
        }

        for (kotlinDir in kotlinDirs) {
            val packageDir = findDeepestSingleChildPath(kotlinDir)
            if (packageDir != kotlinDir) {
                val packagePath = packageDir.relativeTo(kotlinDir).path
                val parts = packagePath.split(File.separator)
                if (parts.size >= 2) {
                    val featureIndex = parts.indexOf("feature")
                    return if (featureIndex > 0) {
                        parts.take(featureIndex).joinToString(".")
                    } else {
                        parts.take(2.coerceAtMost(parts.size)).joinToString(".")
                    }
                }
            }
        }
        return null
    }

    private fun findDeepestSingleChildPath(dir: File): File {
        var current = dir
        while (true) {
            val children = current.listFiles()?.filter { it.isDirectory } ?: break
            if (children.size == 1) {
                current = children[0]
            } else {
                break
            }
        }
        return current
    }

    private fun parseIncludedModules(settingsFile: File): List<String> {
        val content = settingsFile.readText()
        val modules = mutableListOf<String>()
        val pattern = Regex(""""(:[^"]+)"""")
        pattern.findAll(content).forEach { modules.add(it.groupValues[1]) }
        return modules.distinct()
    }

    private fun analyzeModuleStructure(projectRoot: File, modules: List<String>): ModuleStructure {
        val layerCounts = mutableMapOf<String, Int>()
        var resCount = 0
        var featureModuleCount = 0

        val featureModules = modules.filter {
            it.startsWith(":feature:") && it != ":feature:base" && it != ":feature:common"
        }

        for (modulePath in featureModules) {
            val relDir = modulePath.removePrefix(":").replace(':', File.separatorChar)
            val moduleDir = projectRoot.resolve(relDir)
            if (!moduleDir.isDirectory) continue

            featureModuleCount++

            val kotlinDir = findKotlinSourceDir(moduleDir) ?: continue
            val packageDir = findDeepestPackageDir(kotlinDir)

            packageDir.listFiles()
                ?.filter { it.isDirectory }
                ?.forEach { layerDir ->
                    layerCounts[layerDir.name] = (layerCounts[layerDir.name] ?: 0) + 1
                }

            if (moduleDir.resolve("src/main/res").isDirectory) {
                resCount++
            }
        }

        val commonLayers = layerCounts.entries
            .filter { it.value >= featureModuleCount / 2.0 }
            .sortedByDescending { it.value }
            .map { it.key }

        return ModuleStructure(
            layers = commonLayers.ifEmpty { listOf("data", "domain", "presentation") },
            hasRes = resCount > featureModuleCount / 2,
        )
    }

    private fun findKotlinSourceDir(moduleDir: File): File? =
        moduleDir.resolve("src/main/kotlin").takeIf { it.isDirectory }
            ?: moduleDir.resolve("src/main/java").takeIf { it.isDirectory }

    private fun findDeepestPackageDir(sourceRoot: File): File {
        var current = sourceRoot
        while (true) {
            val children = current.listFiles()?.filter { it.isDirectory } ?: break
            if (children.size == 1) {
                current = children[0]
            } else {
                break
            }
        }
        return current
    }
}
