package com.ajd.egsengine.feature.analyzer.data

import com.ajd.egsengine.feature.analyzer.domain.model.ModuleInfo
import com.ajd.egsengine.feature.analyzer.domain.model.ProjectInfo
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.build.BuildEnvironment
import org.slf4j.LoggerFactory
import java.io.File

class GradleProjectScanner(
    private val buildFileParser: BuildFileParser,
) {
    private val logger = LoggerFactory.getLogger(GradleProjectScanner::class.java)

    fun scanProject(projectPath: File): ProjectInfo {
        require(projectPath.isDirectory) { "Project path must be a directory: $projectPath" }

        val settingsFile = projectPath.resolve("settings.gradle.kts").takeIf { it.exists() }
            ?: projectPath.resolve("settings.gradle").takeIf { it.exists() }

        requireNotNull(settingsFile) { "Not a Gradle project (no settings.gradle found): $projectPath" }

        logger.info("Scanning project: ${projectPath.absolutePath}")

        val versionCatalog = buildFileParser.parseVersionCatalog(projectPath)
        val includedModules = buildFileParser.parseSettingsFile(projectPath)

        val toolingApiInfo = tryToolingApi(projectPath)
        val gradleVersion = toolingApiInfo?.gradleVersion ?: detectGradleVersion(projectPath)
        val projectName = toolingApiInfo?.projectName ?: detectProjectName(projectPath)

        val modules = scanModules(projectPath, includedModules)

        return ProjectInfo(
            name = projectName,
            rootPath = projectPath.absolutePath,
            gradleVersion = gradleVersion,
            kotlinVersion = versionCatalog["kotlin"],
            javaVersion = versionCatalog["java"],
            compileSdk = versionCatalog["compile-sdk"],
            minSdk = versionCatalog["min-sdk"],
            targetSdk = versionCatalog["target-sdk"],
            modules = modules,
        )
    }

    private data class ToolingApiResult(
        val projectName: String,
        val gradleVersion: String,
    )

    private fun tryToolingApi(projectPath: File): ToolingApiResult? {
        return try {
            val connector = GradleConnector.newConnector()
                .forProjectDirectory(projectPath)

            connector.connect().use { connection ->
                val buildEnv = connection.getModel(BuildEnvironment::class.java)
                val gradleProject = connection.getModel(GradleProject::class.java)

                ToolingApiResult(
                    projectName = gradleProject.name,
                    gradleVersion = buildEnv.gradle.gradleVersion,
                )
            }
        } catch (e: Exception) {
            logger.warn("Gradle Tooling API connection failed, falling back to file-based analysis: ${e.message}")
            null
        }
    }

    private fun scanModules(projectRoot: File, includedModules: List<String>): List<ModuleInfo> {
        val modules = mutableListOf<ModuleInfo>()

        for (modulePath in includedModules) {
            val relDir = modulePath.removePrefix(":").replace(':', File.separatorChar)
            val moduleDir = projectRoot.resolve(relDir)

            if (!moduleDir.isDirectory) {
                logger.warn("Module directory not found: $moduleDir")
                continue
            }

            val parsed = buildFileParser.parseBuildFile(moduleDir)
            val sourceSetDirs = detectSourceSets(moduleDir)
            val hasManifest = moduleDir.resolve("src/main/AndroidManifest.xml").exists()

            modules.add(
                ModuleInfo(
                    name = modulePath,
                    path = moduleDir.absolutePath,
                    type = parsed?.detectedType ?: com.ajd.egsengine.feature.analyzer.domain.model.ProjectType.UNKNOWN,
                    plugins = parsed?.plugins ?: emptyList(),
                    dependencies = parsed?.dependencies ?: emptyList(),
                    hasAndroidManifest = hasManifest,
                    sourceSetDirs = sourceSetDirs,
                ),
            )
        }

        return modules
    }

    private fun detectSourceSets(moduleDir: File): List<String> {
        val srcDir = moduleDir.resolve("src")
        if (!srcDir.isDirectory) return emptyList()

        return srcDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    private fun detectGradleVersion(projectRoot: File): String {
        val wrapperProps = projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties")
        if (!wrapperProps.exists()) return "unknown"

        val content = wrapperProps.readText()
        val versionPattern = Regex("""gradle-(\d+\.\d+(?:\.\d+)?)-""")
        return versionPattern.find(content)?.groupValues?.get(1) ?: "unknown"
    }

    private fun detectProjectName(projectRoot: File): String {
        val settingsFile = projectRoot.resolve("settings.gradle.kts").takeIf { it.exists() }
            ?: projectRoot.resolve("settings.gradle").takeIf { it.exists() }
            ?: return projectRoot.name

        val content = settingsFile.readText()
        val namePattern = Regex("""rootProject\.name\s*=\s*"([^"]+)"""")
        return namePattern.find(content)?.groupValues?.get(1) ?: projectRoot.name
    }
}
