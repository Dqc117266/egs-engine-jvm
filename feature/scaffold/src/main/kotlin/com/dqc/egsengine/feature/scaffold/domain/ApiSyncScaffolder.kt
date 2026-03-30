package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Cross-platform API sync orchestrator.
 * Reads Swagger JSON from the running backend and dispatches to platform-specific API generators.
 */
class ApiSyncScaffolder(
    private val workspaceResolver: WorkspaceConfigResolver,
    private val swaggerParser: SwaggerParser,
    private val platformApiGenerators: Map<Platform, PlatformApiGenerator>,
) {
    private val logger = LoggerFactory.getLogger(ApiSyncScaffolder::class.java)

    /**
     * Sync API for a specific client module from a specific backend module's Swagger spec.
     */
    fun syncClientApi(
        projectRoot: File,
        clientModuleName: String,
        backendModuleName: String,
        swaggerUrl: String? = null,
        dryRun: Boolean = false,
    ): ApiSyncResult {
        val url = swaggerUrl ?: workspaceResolver.resolveSwaggerUrl(projectRoot)
        val clientConfig = workspaceResolver.resolveClient(projectRoot)

        val spec = swaggerParser.parse(url)

        val gen = platformApiGenerators[clientConfig.platform]
            ?: throw IllegalArgumentException("No API generator for platform: ${clientConfig.platform}")

        val subProjectRoot = projectRoot.resolve(clientConfig.path)
        val generated = gen.generate(subProjectRoot, clientModuleName, spec, clientConfig)

        if (dryRun) {
            return ApiSyncResult(
                clientModule = clientModuleName,
                backendModule = backendModuleName,
                files = generated,
                dryRun = true,
            )
        }

        for (file in generated) {
            val target = subProjectRoot.resolve(file.path)
            target.parentFile.mkdirs()
            file.content?.let { target.writeText(it) }
            logger.debug("Generated: {}", file.path)
        }

        logger.info("Synced API from backend module '{}' to client module '{}' ({} files)",
            backendModuleName, clientModuleName, generated.size)

        return ApiSyncResult(
            clientModule = clientModuleName,
            backendModule = backendModuleName,
            files = generated,
            dryRun = false,
        )
    }

    data class ApiSyncResult(
        val clientModule: String,
        val backendModule: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
