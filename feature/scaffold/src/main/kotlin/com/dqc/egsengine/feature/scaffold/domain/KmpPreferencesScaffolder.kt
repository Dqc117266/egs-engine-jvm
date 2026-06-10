/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpFeatureBuildGradleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPrefsGeneratedDataModuleUpdater
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpPrefsUseCaseGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.KmpPreferencesKotlinEmitter
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFieldParser
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFileMerger
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationMode
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationModeResolver
import org.slf4j.LoggerFactory
import java.io.File

/**
 * `egs client gen prefs` generates TypedPreferenceStore datasource, keys, optional snapshot model, prefs repository slice,
 * combined [TodoRepository] + [TodoRepositoryImpl], and [GeneratedDataModule] prefs block.
 *
 * **MVP snapshot policy:** reusing the same `--key` without `--force` fails if the snapshot model file or key const
 * already exists; use `--force` to overwrite the model and key line.
 */
class KmpPreferencesScaffolder(
    private val workspaceConfigResolver: WorkspaceConfigResolver,
    private val kmpCombinedRepositoryGenerator: KmpCombinedRepositoryGenerator,
    private val kmpRepositoryImplGenerator: KmpRepositoryImplGenerator,
    private val kmpPrefsGeneratedDataModuleUpdater: KmpPrefsGeneratedDataModuleUpdater,
    private val kmpFeatureBuildGradleUpdater: KmpFeatureBuildGradleUpdater,
    private val kmpPrefsUseCaseGenerator: KmpPrefsUseCaseGenerator,
) {
    private val logger = LoggerFactory.getLogger(KmpPreferencesScaffolder::class.java)

    fun scaffoldPrefs(
        projectRoot: File,
        moduleName: String,
        fieldsArg: String,
        keyArg: String?,
        dryRun: Boolean,
        force: Boolean,
    ): KmpPreferencesScaffoldResult {
        val clientConfig = workspaceConfigResolver.resolveClient(projectRoot)
        require(clientConfig.platform == Platform.KMP || clientConfig.platform == Platform.KMP_ANDROID) {
            "client gen prefs requires a KMP client (workspace project 'client' with platform kmp or kmp_android); got ${clientConfig.platform}"
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
        val baseKotlin = "$moduleDir/src/commonMain/kotlin/$pkgPath"

        val keysPath = "$baseKotlin/generate/data/datasource/preferences/model/${pascal}PrefsKeys.kt"
        val dataSourcePath = "$baseKotlin/generate/data/datasource/preferences/$dataSourceClass.kt"
        val prefsRepoPath = "$baseKotlin/generate/domain/repository/$prefsRepoName.kt"
        val prefsSupportPath = "$baseKotlin/generate/data/repository/$prefsSupportName.kt"

        val generated = mutableListOf<GeneratedFile>()

        val keysFile = subProjectRoot.resolve(keysPath)
        val keysContent = PrefsFileMerger.mergeOrCreatePrefsKeys(keysFile, prefsKeysObject, modelPkg, mode, force)
        generated += GeneratedFile(keysPath, keysContent)

        val snapshotClassName: String?
        val snapshotPath: String?
        if (mode is PrefsGenerationMode.Snapshot) {
            snapshotClassName = mode.snapshotClassName
            snapshotPath = "$baseKotlin/generate/data/datasource/preferences/model/$snapshotClassName.kt"
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
        } else {
            snapshotClassName = null
            snapshotPath = null
        }

        val dataSourceFile = subProjectRoot.resolve(dataSourcePath)
        val coreBase = template.basePackage?.let { "$it.core.base" } ?: "template.core.base"
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
                storeImport = "import $coreBase.preferences.TypedPreferenceStore",
            )
        generated += GeneratedFile(dataSourcePath, dsContent)

        val prefsRepoFile = subProjectRoot.resolve(prefsRepoPath)
        val repoIface =
            PrefsFileMerger.mergeOrCreatePrefsRepository(
                prefsRepoFile,
                domainRepoPkg,
                prefsRepoName,
                mode,
                modelPkg,
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
                modelPkg,
            )
        generated += GeneratedFile(prefsSupportPath, supportBody)

        val slices = kmpCombinedRepositoryGenerator.detectSlices(subProjectRoot, moduleName, template)
        kmpCombinedRepositoryGenerator
            .generate(
                template = template,
                subProjectRoot = subProjectRoot,
                includeApi = slices.hasApi,
                includeDb = slices.hasDb,
                includePrefs = true,
            )?.let { generated += it }

        kmpRepositoryImplGenerator
            .generateOrMerge(
                template = template,
                subProjectRoot = subProjectRoot,
                includeApi = slices.hasApi,
                includeDb = slices.hasDb,
                includePrefs = true,
            )?.let { generated += it }

        generated +=
            kmpPrefsUseCaseGenerator.generate(
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
            kmpFeatureBuildGradleUpdater.applyAfterPrefsGen(subProjectRoot, moduleName)
            kmpPrefsGeneratedDataModuleUpdater.apply(subProjectRoot, moduleName, template)
            kmpPrefsGeneratedDataModuleUpdater.updateOriginalDataModule(subProjectRoot, moduleName, template)
            logger.info("KMP prefs scaffold: module '{}' ({} files)", moduleName, generated.size)
        }

        return KmpPreferencesScaffoldResult(
            moduleName = moduleName,
            files = generated,
            dryRun = dryRun,
        )
    }

    data class KmpPreferencesScaffoldResult(
        val moduleName: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
