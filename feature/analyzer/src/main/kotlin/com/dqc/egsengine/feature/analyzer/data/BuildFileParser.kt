package com.dqc.egsengine.feature.analyzer.data

import com.dqc.egsengine.feature.analyzer.domain.model.ProjectType
import org.slf4j.LoggerFactory
import java.io.File

class BuildFileParser {
    private val logger = LoggerFactory.getLogger(BuildFileParser::class.java)

    data class ParsedBuildFile(
        val plugins: List<String>,
        val dependencies: List<String>,
        val detectedType: ProjectType,
        val namespace: String? = null,
    )

    fun parseBuildFile(moduleDir: File): ParsedBuildFile? {
        val buildFile = findBuildFile(moduleDir) ?: return null
        val content = buildFile.readText()

        val plugins = extractPlugins(content)
        val dependencies = extractDependencies(content)
        val type = detectModuleType(content, plugins, moduleDir)
        val namespace = extractNamespace(content)

        logger.debug("Parsed ${buildFile.name}: type=$type, plugins=$plugins")
        return ParsedBuildFile(plugins, dependencies, type, namespace)
    }

    fun parseVersionCatalog(projectRoot: File): Map<String, String> {
        val tomlFile = projectRoot.resolve("gradle/libs.versions.toml")
        if (!tomlFile.exists()) return emptyMap()

        val versions = mutableMapOf<String, String>()
        var inVersionsSection = false

        tomlFile.readLines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed == "[versions]" -> inVersionsSection = true
                trimmed.startsWith("[") -> inVersionsSection = false
                inVersionsSection && trimmed.contains("=") -> {
                    val (key, value) = trimmed.split("=", limit = 2)
                    versions[key.trim()] = value.trim().removeSurrounding("\"")
                }
            }
        }
        return versions
    }

    fun parseSettingsFile(projectRoot: File): List<String> {
        val settingsFile = findSettingsFile(projectRoot) ?: return emptyList()
        val content = settingsFile.readText()
        return extractIncludedModules(content)
    }

    private fun findBuildFile(dir: File): File? =
        dir.resolve("build.gradle.kts").takeIf { it.exists() }
            ?: dir.resolve("build.gradle").takeIf { it.exists() }

    private fun findSettingsFile(dir: File): File? =
        dir.resolve("settings.gradle.kts").takeIf { it.exists() }
            ?: dir.resolve("settings.gradle").takeIf { it.exists() }

    private fun extractPlugins(content: String): List<String> {
        val plugins = mutableListOf<String>()

        // id("plugin.name") or id("plugin.name") apply false
        val idPattern = Regex("""id\s*\(\s*"([^"]+)"\s*\)""")
        idPattern.findAll(content).forEach { plugins.add(it.groupValues[1]) }

        // alias(libs.plugins.xxx)
        val aliasPattern = Regex("""alias\s*\(\s*libs\.plugins\.([a-zA-Z0-9_.]+)\s*\)""")
        aliasPattern.findAll(content).forEach { plugins.add("alias:${it.groupValues[1]}") }

        // kotlin("xxx")
        val kotlinPattern = Regex("""kotlin\s*\(\s*"([^"]+)"\s*\)""")
        kotlinPattern.findAll(content).forEach { plugins.add("org.jetbrains.kotlin.${it.groupValues[1]}") }

        return plugins.distinct()
    }

    private fun extractDependencies(content: String): List<String> {
        val deps = mutableListOf<String>()

        // implementation(project(":xxx"))
        val projectDepPattern = Regex("""(?:implementation|api)\s*\(\s*project\s*\(\s*"([^"]+)"\s*\)\s*\)""")
        projectDepPattern.findAll(content).forEach { deps.add(it.groupValues[1]) }

        // implementation(projects.xxx.yyy)
        val typeSafePattern = Regex("""(?:implementation|api)\s*\(\s*projects\.([a-zA-Z0-9_.]+)\s*\)""")
        typeSafePattern.findAll(content).forEach { deps.add(":${it.groupValues[1].replace('.', ':')}") }

        return deps.distinct()
    }

    private fun extractNamespace(content: String): String? {
        val nsPattern = Regex("""namespace\s*=\s*"([^"]+)"""")
        return nsPattern.find(content)?.groupValues?.get(1)
    }

    private fun extractIncludedModules(content: String): List<String> {
        val modules = mutableListOf<String>()

        // include(":module:name") or include(":module")
        val includePattern = Regex(""""(:[^"]+)"""")
        includePattern.findAll(content).forEach { modules.add(it.groupValues[1]) }

        return modules.distinct()
    }

    private fun detectModuleType(content: String, plugins: List<String>, moduleDir: File): ProjectType {
        val allPlugins = plugins.joinToString(" ").lowercase()
        val contentLower = content.lowercase()

        return when {
            allPlugins.contains("com.android.application") ||
                allPlugins.contains("android.application") ||
                contentLower.contains("com.android.application") -> ProjectType.ANDROID_APPLICATION

            allPlugins.contains("com.android.library") ||
                allPlugins.contains("android.library") ||
                contentLower.contains("com.android.library") -> ProjectType.ANDROID_LIBRARY

            allPlugins.contains("kotlin-multiplatform") ||
                allPlugins.contains("multiplatform") ||
                contentLower.contains("kotlin-multiplatform") ||
                contentLower.contains("org.jetbrains.kotlin.multiplatform") ||
                moduleDir.resolve("src/commonMain").isDirectory -> ProjectType.KOTLIN_MULTIPLATFORM

            allPlugins.contains("org.jetbrains.kotlin.jvm") ||
                allPlugins.contains("kotlin.jvm") ||
                contentLower.contains("kotlin(\"jvm\")") -> ProjectType.KOTLIN_JVM

            allPlugins.contains("java") ||
                allPlugins.contains("java-library") ||
                contentLower.contains("java-library") -> ProjectType.JAVA

            moduleDir.resolve("src/main/kotlin").isDirectory -> ProjectType.KOTLIN_JVM
            moduleDir.resolve("src/main/java").isDirectory -> ProjectType.JAVA

            else -> ProjectType.UNKNOWN
        }
    }
}
