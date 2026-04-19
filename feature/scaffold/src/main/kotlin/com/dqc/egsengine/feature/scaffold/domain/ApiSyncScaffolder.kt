package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformApiGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.PlatformModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiSyncKoinUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpApiSyncKoinUpdater
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
    private val platformModuleGenerators: Map<Platform, PlatformModuleGenerator>,
    private val kmpApiSyncKoinUpdater: KmpApiSyncKoinUpdater,
    private val androidApiSyncKoinUpdater: AndroidApiSyncKoinUpdater,
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
        val url = swaggerUrl ?: workspaceResolver.resolveSwaggerUrl(projectRoot, clientModuleName)
        val clientConfig = workspaceResolver.resolveClient(projectRoot)

        val spec = swaggerParser.parse(url)

        val gen = platformApiGenerators[clientConfig.platform]
            ?: throw IllegalArgumentException("No API generator for platform: ${clientConfig.platform}")

        val subProjectRoot = projectRoot.resolve(clientConfig.path)
        val scaffoldFiles = if (!dryRun) {
            ensureFeatureModuleScaffold(
                projectRoot = projectRoot,
                subProjectRoot = subProjectRoot,
                clientModuleName = clientModuleName,
                clientConfig = clientConfig,
            )
        } else {
            emptyList()
        }

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

        kmpApiSyncKoinUpdater.applyAfterSync(
            subProjectRoot = subProjectRoot,
            moduleName = clientModuleName,
            config = clientConfig,
        )
        androidApiSyncKoinUpdater.applyAfterSync(
            subProjectRoot = subProjectRoot,
            moduleName = clientModuleName,
            config = clientConfig,
        )

        logger.info("Synced API from backend module '{}' to client module '{}' ({} files, scaffold={})",
            backendModuleName, clientModuleName, generated.size, scaffoldFiles.size)

        return ApiSyncResult(
            clientModule = clientModuleName,
            backendModule = backendModuleName,
            files = generated,
            dryRun = false,
        )
    }

    /**
     * Auto-creates the feature module skeleton (`build.gradle.kts`, `AndroidManifest.xml` on
     * Android, the Koin/Data/Domain/Presentation stubs) and registers it in
     * `settings.gradle.kts` when the target feature directory does not already carry a Gradle
     * build file.
     *
     * Writes only files that do not already exist so that hand-edits inside
     * `feature/<module>/` (e.g. a user-authored `RepositoryImpl`) survive re-runs. No-op when
     * no module generator is registered for the client's platform, or when the feature already
     * has a `build.gradle.kts`.
     */
    private fun ensureFeatureModuleScaffold(
        projectRoot: File,
        subProjectRoot: File,
        clientModuleName: String,
        clientConfig: com.dqc.egsengine.feature.init.domain.model.SubProjectConfig,
    ): List<File> {
        val featureBuildFile = subProjectRoot.resolve("feature/$clientModuleName/build.gradle.kts")
        if (featureBuildFile.exists()) return emptyList()

        val moduleGen = platformModuleGenerators[clientConfig.platform] ?: run {
            logger.debug(
                "No module generator registered for platform {} - skipping scaffold auto-create for feature/{}",
                clientConfig.platform,
                clientModuleName,
            )
            return emptyList()
        }

        logger.info(
            "feature/{} build file missing; auto-scaffolding module skeleton before API sync",
            clientModuleName,
        )
        val preview = moduleGen.preview(subProjectRoot, clientModuleName, clientConfig)
        val created = mutableListOf<File>()
        preview.forEach { entry ->
            val target = subProjectRoot.resolve(entry.path)
            if (target.exists()) {
                logger.debug("Skip existing file during scaffold auto-create: {}", target.path)
                return@forEach
            }
            target.parentFile.mkdirs()
            val content = entry.content
            if (content != null) {
                target.writeText(content)
            } else {
                target.createNewFile()
            }
            created.add(target)
        }
        moduleGen.updateSettings(projectRoot, clientModuleName, clientConfig)
        return created
    }

    data class ApiSyncResult(
        val clientModule: String,
        val backendModule: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
