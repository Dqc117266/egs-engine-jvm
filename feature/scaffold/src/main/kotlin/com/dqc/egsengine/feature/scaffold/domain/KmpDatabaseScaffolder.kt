/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseCachedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseDbOnlyDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseEntityMapperGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpDatabaseRepositoryUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Parses DDL and writes KMP Room database sources under the client subproject.
 */
class KmpDatabaseScaffolder(
    private val ddlParser: DdlParser,
    private val kmpDatabaseCodeGenerator: KmpDatabaseCodeGenerator,
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val kmpFeatureBuildGradleUpdater: KmpFeatureBuildGradleUpdater,
    private val kmpDatabaseGeneratedDataModuleUpdater: KmpDatabaseGeneratedDataModuleUpdater,
    private val kmpDatabaseRepositoryGenerator: KmpDatabaseRepositoryGenerator,
    private val kmpDatabaseDbOnlyDataModuleUpdater: KmpDatabaseDbOnlyDataModuleUpdater,
    private val kmpDatabaseRepositoryUpdater: KmpDatabaseRepositoryUpdater,
    private val kmpDatabaseEntityMapperGenerator: KmpDatabaseEntityMapperGenerator,
    private val kmpDatabaseCachedRepositoryGenerator: KmpDatabaseCachedRepositoryGenerator,
    private val kmpSwaggerCodeGenerator: KmpSwaggerCodeGenerator,
    private val swaggerParser: SwaggerParser,
) {
    private val logger = LoggerFactory.getLogger(KmpDatabaseScaffolder::class.java)

    fun scaffoldDatabase(
        projectRoot: File,
        sqlFile: File,
        moduleName: String,
        dryRun: Boolean = false,
        repo: Boolean = false,
        cached: Boolean = false,
    ): KmpDatabaseScaffoldResult {
        val clientConfig = workspaceConfigResolver.resolveClient(projectRoot)
        require(clientConfig.platform == Platform.KMP || clientConfig.platform == Platform.KMP_ANDROID) {
            "client gen database requires a KMP client (workspace project 'client' with platform kmp or kmp_android); got ${clientConfig.platform}"
        }

        val tables = ddlParser.parseFile(sqlFile)
        val template = clientConfig.toModuleTemplate(moduleName)
        val generated = kmpDatabaseCodeGenerator.generate(template, tables, projectRoot).toMutableList()

        val subProjectRoot = projectRoot.resolve(clientConfig.path)
        val pkgPath = template.packageName.replace('.', '/')
        val modulePascal = SqlNaming.moduleNameToPascal(moduleName)
        val supportFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/generate/data/repository/Generated${modulePascal}RepositorySupport.kt",
        )
        val hasApi = supportFile.exists()

        val effectiveRepo = repo || cached
        if (cached) {
            require(hasApi) {
                "client gen database --cached requires an existing API sync (Generated${modulePascal}RepositorySupport.kt not found)."
            }
        }

        if (effectiveRepo) {
            when {
                cached && hasApi -> {
                    val spec = swaggerParser.parse(workspaceConfigResolver.resolveSwaggerUrl(projectRoot))
                    generated += kmpDatabaseEntityMapperGenerator.generate(template, tables, spec, projectRoot)
                    kmpDatabaseCachedRepositoryGenerator.generateCachedRepositorySupport(
                        template,
                        tables,
                        kmpSwaggerCodeGenerator.adjustSpecForKmp(spec),
                        projectRoot,
                    )?.let { generated += it }
                }
                !cached && hasApi -> Unit
                !cached && !hasApi -> {
                    generated += kmpDatabaseRepositoryGenerator.generateDbOnlyRepository(template, tables, projectRoot)
                }
            }
        }

        if (!dryRun) {
            for (file in generated) {
                val target = subProjectRoot.resolve(file.path)
                target.parentFile.mkdirs()
                file.content?.let { target.writeText(it) }
                logger.debug("Wrote {}", file.path)
            }
            kmpFeatureBuildGradleUpdater.applyAfterDatabaseGen(subProjectRoot, moduleName)
            kmpDatabaseGeneratedDataModuleUpdater.apply(subProjectRoot, moduleName, template, tables)
            if (effectiveRepo && hasApi) {
                kmpDatabaseRepositoryUpdater.apply(subProjectRoot, moduleName, template)
            }
            if (effectiveRepo && !hasApi) {
                kmpDatabaseDbOnlyDataModuleUpdater.apply(subProjectRoot, moduleName, template)
            }
            logger.info(
                "KMP database scaffold: {} table(s) -> module '{}' ({} files)",
                tables.size,
                moduleName,
                generated.size,
            )
        }

        return KmpDatabaseScaffoldResult(
            moduleName = moduleName,
            files = generated,
            dryRun = dryRun,
        )
    }

    data class KmpDatabaseScaffoldResult(
        val moduleName: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
