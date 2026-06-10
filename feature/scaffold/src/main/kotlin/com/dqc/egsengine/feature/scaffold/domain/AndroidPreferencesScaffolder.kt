/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDbOnlyRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidPrefsGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidPrefsUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.KmpPreferencesKotlinEmitter
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFieldParser
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFileMerger
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationMode
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationModeResolver
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Android client: same prefs layout as [KmpPreferencesScaffolder] under `src/main/kotlin`.
 */
class AndroidPreferencesScaffolder(
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val androidCombinedRepositoryGenerator: AndroidCombinedRepositoryGenerator,
    private val androidDbOnlyRepositoryImplGenerator: AndroidDbOnlyRepositoryImplGenerator,
    private val androidPrefsGeneratedDataModuleUpdater: AndroidPrefsGeneratedDataModuleUpdater,
    private val androidFeatureBuildGradleUpdater: AndroidFeatureBuildGradleUpdater,
    private val androidPrefsUseCaseGenerator: AndroidPrefsUseCaseGenerator,
) {
    private val logger = LoggerFactory.getLogger(AndroidPreferencesScaffolder::class.java)

    fun scaffoldPrefs(
        projectRoot: File,
        moduleName: String,
        fieldsArg: String,
        keyArg: String?,
        dryRun: Boolean,
        force: Boolean,
    ): KmpPreferencesScaffolder.KmpPreferencesScaffoldResult {
        val clientConfig = workspaceConfigResolver.resolveClient(projectRoot)
        require(clientConfig.platform == Platform.ANDROID) {
            "client gen prefs (Android path) requires workspace project 'client' with platform android; got ${clientConfig.platform}"
        }

        val parsed = PrefsFieldParser.parseFields(fieldsArg)
        val mode = PrefsGenerationModeResolver.resolve(keyArg, parsed)
        val template = clientConfig.toModuleTemplate(moduleName)
        val subProjectRoot = projectRoot.resolve(clientConfig.path)
        val pkg = template.packageName
        val pascal = SqlNaming.moduleNameToPascal(moduleName)
        val pkgPath = pkg.replace('.', '/')
        val modelPkg = "$pkg.generate.data.datasource.preferences.model"
        val prefsPkg = "$pkg.generate.data.datasource.preferences"
        val domainRepoPkg = "$pkg.generate.domain.repository"
        val dataRepoPkg = "$pkg.generate.data.repository"
        val prefsKeysObject = "${pascal}PrefsKeys"
        val prefsKeysFq = "$modelPkg.$prefsKeysObject"
        val dataSourceClass = "${pascal}PreferencesDataSource"
        val prefsRepoName = "${pascal}PrefsRepository"
        val prefsSupportName = "Generated${pascal}PrefsRepositorySupport"

        val moduleDir = "feature/$moduleName"
        val baseKotlin = "$moduleDir/src/main/kotlin/$pkgPath"

        val keysPath = "$baseKotlin/generate/data/datasource/preferences/model/${pascal}PrefsKeys.kt"
        val dataSourcePath = "$baseKotlin/generate/data/datasource/preferences/$dataSourceClass.kt"
        val prefsRepoPath = "$baseKotlin/generate/domain/repository/$prefsRepoName.kt"
        val prefsSupportPath = "$baseKotlin/generate/data/repository/$prefsSupportName.kt"

        val generated = mutableListOf<GeneratedFile>()

        val keysFile = subProjectRoot.resolve(keysPath)
        val keysContent = PrefsFileMerger.mergeOrCreatePrefsKeys(keysFile, prefsKeysObject, modelPkg, mode, force)
        generated += GeneratedFile(keysPath, keysContent)

        if (mode is PrefsGenerationMode.Snapshot) {
            val snapshotClassName = mode.snapshotClassName
            val snapshotPath = "$baseKotlin/generate/data/datasource/preferences/model/$snapshotClassName.kt"
            val snapFile = subProjectRoot.resolve(snapshotPath)
            if (snapFile.exists() && !force) {
                throw IllegalStateException(
                    "Snapshot model ${snapFile.name} already exists. Re-run with --force to overwrite " +
                        "(MVP: snapshot keys should be generated once; merging data classes is not supported without --force).",
                )
            }
            val modelBody = KmpPreferencesKotlinEmitter.snapshotSerializableModel(mode.snapshotClassName, mode.fields)
            val snapshotContent = PrefsFileMerger.renderSnapshotModelFile(modelPkg, snapshotClassName, modelBody)
            generated += GeneratedFile(snapshotPath, snapshotContent)
        }

        val dataSourceFile = subProjectRoot.resolve(dataSourcePath)
        val dsContent =
            PrefsFileMerger.mergeOrCreateDataSource(
                existingFile = dataSourceFile,
                prefsPkg = prefsPkg,
                modelPkg = modelPkg,
                dataSourceClass = dataSourceClass,
                prefsKeysObject = prefsKeysObject,
                prefsKeysFq = prefsKeysFq,
                mode = mode,
                force = force,
                storeImport = "import template.core.base.preferences.TypedPreferenceStore",
            )
        generated += GeneratedFile(dataSourcePath, dsContent)

        val prefsRepoFile = subProjectRoot.resolve(prefsRepoPath)
        val repoIface =
            PrefsFileMerger.mergeOrCreatePrefsRepository(
                prefsRepoFile,
                domainRepoPkg,
                prefsRepoName,
                mode,
            )
        generated += GeneratedFile(prefsRepoPath, repoIface)

        val supportFile = subProjectRoot.resolve(prefsSupportPath)
        val supportBody =
            PrefsFileMerger.mergeOrCreatePrefsSupport(
                supportFile,
                dataRepoPkg,
                domainRepoPkg,
                prefsSupportName,
                prefsRepoName,
                dataSourceClass,
                prefsPkg,
                mode,
            )
        generated += GeneratedFile(prefsSupportPath, supportBody)

        val slices = androidCombinedRepositoryGenerator.detectSlices(subProjectRoot, moduleName, template)
        androidCombinedRepositoryGenerator
            .generate(
                template = template,
                subProjectRoot = subProjectRoot,
                includeApi = slices.hasApi,
                includeDb = slices.hasDb,
                includePrefs = true,
            )?.let { generated += it }

        androidDbOnlyRepositoryImplGenerator
            .generateOrMerge(
                template = template,
                subProjectRoot = subProjectRoot,
                includeApi = slices.hasApi,
                includeDb = slices.hasDb,
                includePrefs = true,
            )?.let { generated += it }

        generated +=
            androidPrefsUseCaseGenerator.generate(
                template = template,
                mode = mode,
                projectRoot = projectRoot,
                subProjectRoot = subProjectRoot,
            )

        if (!dryRun) {
            for (file in generated) {
                val target = subProjectRoot.resolve(file.path)
                target.parentFile.mkdirs()
                file.content?.let { target.writeText(it) }
                logger.debug("Wrote {}", file.path)
            }
            androidFeatureBuildGradleUpdater.applyAfterPrefsGen(subProjectRoot, moduleName)
            androidPrefsGeneratedDataModuleUpdater.apply(subProjectRoot, moduleName, template)
            logger.info("Android prefs scaffold: module '{}' ({} files)", moduleName, generated.size)
        }

        return KmpPreferencesScaffolder.KmpPreferencesScaffoldResult(
            moduleName = moduleName,
            files = generated,
            dryRun = dryRun,
        )
    }
}
