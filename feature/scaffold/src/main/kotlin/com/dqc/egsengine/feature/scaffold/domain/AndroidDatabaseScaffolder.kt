/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidApiDbRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseCodeGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseDbOnlyDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseEntityMapperGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDatabaseUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDbOnlyRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerParser
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Android client: DDL ¡ú Room + optional DB repository / use cases (`src/main/kotlin`).
 * Independent from [KmpDatabaseScaffolder].
 */
class AndroidDatabaseScaffolder(
    private val ddlParser: DdlParser,
    private val androidDatabaseCodeGenerator: AndroidDatabaseCodeGenerator,
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val androidDatabaseGeneratedDataModuleUpdater: AndroidDatabaseGeneratedDataModuleUpdater,
    private val androidDatabaseRepositoryGenerator: AndroidDatabaseRepositoryGenerator,
    private val androidDatabaseDbOnlyDataModuleUpdater: AndroidDatabaseDbOnlyDataModuleUpdater,
    private val androidDatabaseEntityMapperGenerator: AndroidDatabaseEntityMapperGenerator,
    private val androidDatabaseUseCaseGenerator: AndroidDatabaseUseCaseGenerator,
    private val swaggerParser: SwaggerParser,
    private val androidCombinedRepositoryGenerator: AndroidCombinedRepositoryGenerator,
    private val androidDbOnlyRepositoryImplGenerator: AndroidDbOnlyRepositoryImplGenerator,
    private val androidApiDbRepositoryImplGenerator: AndroidApiDbRepositoryImplGenerator,
    private val androidFeatureBuildGradleUpdater: AndroidFeatureBuildGradleUpdater,
) {
    private val logger = LoggerFactory.getLogger(AndroidDatabaseScaffolder::class.java)

    fun scaffoldDatabase(
        projectRoot: File,
        sqlFile: File,
        moduleName: String,
        dryRun: Boolean = false,
        repo: Boolean = false,
        cached: Boolean = false,
    ): KmpDatabaseScaffolder.KmpDatabaseScaffoldResult {
        val tables = ddlParser.parseFile(sqlFile)
        val template = workspaceConfigResolver.resolveClient(projectRoot).let { cfg ->
            cfg.toModuleTemplate(moduleName)
        }
        val generated = androidDatabaseCodeGenerator.generate(template, tables, projectRoot).toMutableList()

        val clientConfig = workspaceConfigResolver.resolveClient(projectRoot)
        val subProjectRoot = projectRoot.resolve(clientConfig.path)
        val modulePascal = SqlNaming.moduleNameToPascal(moduleName)
        val pkgPath = template.packageName.replace('.', '/')
        val androidApiSupport = subProjectRoot.resolve(
            "feature/$moduleName/src/main/kotlin/$pkgPath/generate/data/repository/Generated${modulePascal}ApiRepositorySupport.kt",
        )
        val hasApi = androidApiSupport.exists()

        val swaggerSpec = runCatching {
            swaggerParser.parse(workspaceConfigResolver.resolveSwaggerUrl(projectRoot))
        }.onFailure { logger.debug("Swagger not available for DB VO mapping: {}", it.message) }
            .getOrNull()

        if (swaggerSpec != null) {
            generated += androidDatabaseEntityMapperGenerator.generate(template, tables, swaggerSpec, projectRoot)
        }

        val effectiveRepo = repo || cached
        if (cached) {
            require(hasApi) {
                "client gen database --cached requires an existing API sync (Generated${modulePascal}ApiRepositorySupport.kt not found)."
            }
        }

        if (cached && hasApi) {
            logger.warn(
                "--cached is deprecated: cache-aside belongs in {}RepositoryImpl overrides (delegation model).",
                modulePascal,
            )
        }

        if (effectiveRepo) {
            when {
                !cached && hasApi -> {
                    generated += androidDatabaseRepositoryGenerator.generateDbOnlyRepository(
                        template,
                        tables,
                        projectRoot,
                        swaggerSpec,
                    )
                    generated += androidDatabaseUseCaseGenerator.generate(
                        template = template,
                        tables = tables,
                        projectRoot = projectRoot,
                        subProjectRoot = subProjectRoot,
                        useCaseRepositorySimpleName = "${modulePascal}DbRepository",
                        spec = swaggerSpec,
                    )
                    androidApiDbRepositoryImplGenerator.generateOrMerge(template, subProjectRoot)?.let { generated += it }
                }
                !cached && !hasApi -> {
                    generated += androidDatabaseRepositoryGenerator.generateDbOnlyRepository(
                        template,
                        tables,
                        projectRoot,
                        swaggerSpec,
                    )
                    generated += androidDatabaseUseCaseGenerator.generate(
                        template = template,
                        tables = tables,
                        projectRoot = projectRoot,
                        subProjectRoot = subProjectRoot,
                        spec = swaggerSpec,
                    )
                    val prefsSlice = androidCombinedRepositoryGenerator.detectSlices(
                        subProjectRoot,
                        moduleName,
                        template,
                    ).hasPrefs
                    androidCombinedRepositoryGenerator.generate(
                        template = template,
                        subProjectRoot = subProjectRoot,
                        includeApi = false,
                        includeDb = true,
                        includePrefs = prefsSlice,
                    )?.let { generated += it }
                    androidDbOnlyRepositoryImplGenerator.generateOrMerge(
                        template = template,
                        subProjectRoot = subProjectRoot,
                        includeApi = false,
                        includeDb = true,
                        includePrefs = prefsSlice,
                    )?.let { generated += it }
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
            androidFeatureBuildGradleUpdater.applyAfterDatabaseGen(subProjectRoot, moduleName)
            val includeDbRepositorySupport = effectiveRepo && !cached
            androidDatabaseGeneratedDataModuleUpdater.apply(
                subProjectRoot,
                moduleName,
                template,
                tables,
                includeDbRepositorySupport = includeDbRepositorySupport,
            )
            if (effectiveRepo && !hasApi) {
                androidDatabaseDbOnlyDataModuleUpdater.apply(subProjectRoot, moduleName, template)
            }
            logger.info(
                "Android database scaffold: {} table(s) -> module '{}' ({} files)",
                tables.size,
                moduleName,
                generated.size,
            )
        }

        return KmpDatabaseScaffolder.KmpDatabaseScaffoldResult(
            moduleName = moduleName,
            files = generated,
            dryRun = dryRun,
        )
    }
}
