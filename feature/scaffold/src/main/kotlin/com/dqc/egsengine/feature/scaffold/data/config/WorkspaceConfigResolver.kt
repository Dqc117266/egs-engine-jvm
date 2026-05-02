package com.dqc.egsengine.feature.scaffold.data.config

import com.dqc.egsengine.feature.init.data.WorkspaceConfigReader
import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.init.domain.model.WorkspaceConfig
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Scaffold-layer helper that reads workspace config and resolves the correct
 * [SubProjectConfig] for a given target platform or project key.
 */
class WorkspaceConfigResolver(
    private val workspaceConfigReader: WorkspaceConfigReader,
) {
    private val logger = LoggerFactory.getLogger(WorkspaceConfigResolver::class.java)

    fun readWorkspace(projectRoot: File): WorkspaceConfig =
        workspaceConfigReader.read(projectRoot)

    fun isWorkspaceProject(projectRoot: File): Boolean =
        workspaceConfigReader.hasWorkspaceConfig(projectRoot)

    fun resolveByKey(projectRoot: File, projectKey: String): SubProjectConfig {
        val workspace = readWorkspace(projectRoot)
        return workspace.projects[projectKey]
            ?: throw IllegalArgumentException(
                "No project '$projectKey' found in workspace.json. Available: ${workspace.projects.keys}",
            )
    }

    fun resolveByPlatform(projectRoot: File, platform: Platform): SubProjectConfig {
        val workspace = readWorkspace(projectRoot)
        return workspace.projects.values.firstOrNull { it.platform == platform }
            ?: throw IllegalArgumentException(
                "No project with platform '$platform' found in workspace.json.",
            )
    }

    fun resolveClient(projectRoot: File): SubProjectConfig =
        resolveByKey(projectRoot, "client")

    fun resolveBackend(projectRoot: File): SubProjectConfig =
        resolveByKey(projectRoot, "backend")

    fun resolveEffectiveBackendConfig(projectRoot: File): SubProjectConfig {
        val config = resolveBackend(projectRoot)
        if (config.platform != Platform.SPRING_BOOT) return config
        val backendRoot = projectRoot.resolve(config.path)
        val detectedBasePackage = detectSpringBootBasePackage(backendRoot)
        val detectedConventionPluginId = detectSpringBootConventionPluginId(backendRoot)

        if (!detectedBasePackage.isNullOrBlank() && detectedBasePackage != config.basePackage) {
            logger.warn(
                "Backend basePackage '{}' differs from detected Spring package '{}'; using detected value",
                config.basePackage,
                detectedBasePackage,
            )
        }
        if (!detectedConventionPluginId.isNullOrBlank() && detectedConventionPluginId != config.conventionPluginId) {
            logger.warn(
                "Backend conventionPluginId '{}' differs from detected plugin '{}'; using detected value",
                config.conventionPluginId,
                detectedConventionPluginId,
            )
        }

        return config.copy(
            basePackage = detectedBasePackage?.takeIf { it.isNotBlank() } ?: config.basePackage,
            conventionPluginId = detectedConventionPluginId?.takeIf { it.isNotBlank() } ?: config.conventionPluginId,
        )
    }

    fun resolveAdmin(projectRoot: File): SubProjectConfig =
        resolveByKey(projectRoot, "admin")

    fun resolveSwaggerUrl(projectRoot: File): String {
        val workspace = readWorkspace(projectRoot)
        val swagger = workspace.swagger
            ?: throw IllegalStateException("No swagger sync config in workspace.json")
        return joinSwaggerUrl(swagger.baseUrl, swagger.docPath)
    }

    /**
     * Resolves Swagger URL for a client feature module using [SwaggerSyncConfig.modules] when present.
     * Priority: per-module [SwaggerModuleConfig.url] > per-module [SwaggerModuleConfig.docPath] > global doc path.
     */
    fun resolveSwaggerUrl(projectRoot: File, clientModule: String): String {
        val workspace = readWorkspace(projectRoot)
        val swagger = workspace.swagger
            ?: throw IllegalStateException("No swagger sync config in workspace.json")
        val mod = swagger.modules[clientModule]
        when {
            !mod?.url.isNullOrBlank() -> return mod!!.url!!.trim()
            !mod?.docPath.isNullOrBlank() -> {
                val p = mod!!.docPath!!.trim()
                return if (p.startsWith("http://") || p.startsWith("https://")) {
                    p
                } else {
                    joinSwaggerUrl(swagger.baseUrl, p)
                }
            }
            else -> return joinSwaggerUrl(swagger.baseUrl, swagger.docPath)
        }
    }

    private fun joinSwaggerUrl(baseUrl: String, docPath: String): String {
        val base = baseUrl.trimEnd('/')
        val path = if (docPath.startsWith("/")) docPath else "/$docPath"
        return base + path
    }

    fun detectSpringBootBasePackage(backendRoot: File): String? {
        val appSourceRoot = backendRoot.resolve("app/src/main/kotlin")
        val appFile = findSpringBootApplicationFile(appSourceRoot) ?: return detectFeaturePackageRoot(backendRoot)
        val text = appFile.readText()
        val fromScan = extractScanBasePackages(text).firstOrNull()
        if (!fromScan.isNullOrBlank()) return fromScan
        val packageName = extractPackageName(text) ?: return detectFeaturePackageRoot(backendRoot)
        return packageName.substringBeforeLast('.', packageName)
    }

    fun detectSpringBootConventionPluginId(backendRoot: File): String? {
        val featureDir = backendRoot.resolve("feature")
        val featureBuild = featureDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.resolve("build.gradle.kts") }
            ?.firstOrNull { it.exists() }
        val file = featureBuild ?: return null
        val text = file.readText()
        return Regex("""id\(\"([^\"]+)\"\)""")
            .findAll(text)
            .map { it.groupValues[1] }
            .firstOrNull { it.contains("convention.feature") }
    }

    private fun findSpringBootApplicationFile(appSourceRoot: File): File? {
        if (!appSourceRoot.isDirectory) return null
        return appSourceRoot.walkTopDown()
            .firstOrNull { file ->
                file.isFile &&
                    file.extension == "kt" &&
                    file.readText().contains("@SpringBootApplication")
            }
    }

    private fun extractPackageName(text: String): String? =
        Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)

    private fun extractScanBasePackages(text: String): List<String> =
        Regex("""scanBasePackages\s*=\s*\[(.*?)]""", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { body ->
                Regex("""\"([^\"]+)\"""")
                    .findAll(body)
                    .map { it.groupValues[1] }
                    .toList()
            }
            ?: emptyList()

    private fun detectFeaturePackageRoot(backendRoot: File): String? {
        val featureRoot = backendRoot.resolve("feature")
        if (!featureRoot.isDirectory) return null
        val firstKt = featureRoot.walkTopDown().firstOrNull { it.isFile && it.extension == "kt" } ?: return null
        val relative = firstKt.parentFile?.relativeTo(featureRoot)?.invariantSeparatorsPath ?: return null
        val packagePath = relative.substringAfter("src/main/kotlin/", missingDelimiterValue = "")
        if (packagePath.isBlank()) return null
        val segments = packagePath.split('/').takeWhile { it != "feature" }
        return segments.joinToString(".").takeIf { it.isNotBlank() }
    }
}
