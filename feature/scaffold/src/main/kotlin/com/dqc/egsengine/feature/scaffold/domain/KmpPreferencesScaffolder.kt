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
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.KmpPreferencesBlockMerger
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.KmpPreferencesKotlinEmitter
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFieldParser
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
        val keysContent = mergeOrCreatePrefsKeys(keysFile, prefsKeysObject, modelPkg, mode, force)
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
            val snapshotContent = renderSnapshotModelFile(modelPkg, snapshotClassName, modelBody)
            generated += GeneratedFile(snapshotPath, snapshotContent)
        } else {
            snapshotClassName = null
            snapshotPath = null
        }

        val dataSourceFile = subProjectRoot.resolve(dataSourcePath)
        val coreBase = template.basePackage?.let { "${it}.core.base" } ?: "template.core.base"
        val dsContent = mergeOrCreateDataSource(
            existingFile = dataSourceFile,
            prefsPkg = prefsPkg,
            modelPkg = modelPkg,
            dataSourceClass = dataSourceClass,
            prefsKeysObject = prefsKeysObject,
            prefsKeysFq = prefsKeysFq,
            mode = mode,
            force = force,
            coreBase = coreBase,
        )
        generated += GeneratedFile(dataSourcePath, dsContent)

        val prefsRepoFile = subProjectRoot.resolve(prefsRepoPath)
        val repoIface = mergeOrCreatePrefsRepository(
            prefsRepoFile,
            domainRepoPkg,
            prefsRepoName,
            mode,
        )
        generated += GeneratedFile(prefsRepoPath, repoIface)

        val supportFile = subProjectRoot.resolve(prefsSupportPath)
        val supportBody = mergeOrCreatePrefsSupport(
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

        val slices = kmpCombinedRepositoryGenerator.detectSlices(subProjectRoot, moduleName, template)
        kmpCombinedRepositoryGenerator.generate(
            template = template,
            subProjectRoot = subProjectRoot,
            includeApi = slices.hasApi,
            includeDb = slices.hasDb,
            includePrefs = true,
        )?.let { generated += it }

        kmpRepositoryImplGenerator.generateOrMerge(
            template = template,
            subProjectRoot = subProjectRoot,
            includeApi = slices.hasApi,
            includeDb = slices.hasDb,
            includePrefs = true,
        )?.let { generated += it }

        generated += kmpPrefsUseCaseGenerator.generate(
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
            logger.info("KMP prefs scaffold: module '{}' ({} files)", moduleName, generated.size)
        }

        return KmpPreferencesScaffoldResult(
            moduleName = moduleName,
            files = generated,
            dryRun = dryRun,
        )
    }

    private fun renderSnapshotModelFile(modelPkg: String, className: String, body: String): String =
        """
        /*
         * Copyright 2026 Mifos Initiative
         *
         * SPDX-License-Identifier: MPL-2.0
         */
        package $modelPkg

        ${body.trim()}
        """.trimIndent() + "\n"

    /** Indents a generated block for `object Scalar` / `Snapshot` bodies (keys file). */
    private fun indentPrefsKeysBlock(text: String): String = text.prependIndent("        ")

    /** Indents generated members inside `internal class` / `internal interface` (4 spaces). */
    private fun indentClassMemberBlock(text: String): String = text.trimEnd().prependIndent("    ")

    private fun mergeOrCreatePrefsKeys(
        file: File,
        prefsKeysObject: String,
        modelPkg: String,
        mode: PrefsGenerationMode,
        force: Boolean,
    ): String {
        return when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val upper = KmpPreferencesKotlinEmitter.upperConstFromLogicalKey(mode.logicalKey)
                val chunk = KmpPreferencesKotlinEmitter.scalarPrefsKeysLines(mode.logicalKey, mode.field)
                if (!file.exists()) {
                    renderPrefsKeysFull(modelPkg, prefsKeysObject, scalarInner = chunk, snapshotInner = "")
                } else {
                    var text = file.readText()
                    if (KmpPreferencesBlockMerger.hasConstName(text, upper)) {
                        if (force) {
                            text = replaceScalarKeyConst(text, upper, chunk)
                        } else {
                            logger.info("Scalar key {} already present in {}; skipping keys merge", upper, file.path)
                            return text
                        }
                    } else {
                        text = KmpPreferencesBlockMerger.appendToMarkedBlock(
                            text,
                            KmpPreferencesBlockMerger.SCALAR_KEYS_BEGIN,
                            KmpPreferencesBlockMerger.SCALAR_KEYS_END,
                            indentPrefsKeysBlock(chunk),
                            shouldSkipAppend = { false },
                        )
                    }
                    text
                }
            }
            is PrefsGenerationMode.Snapshot -> {
                val upper = KmpPreferencesKotlinEmitter.upperConstFromLogicalKey(mode.logicalKey)
                val chunk = KmpPreferencesKotlinEmitter.snapshotPrefsKeyLine(mode.logicalKey)
                if (!file.exists()) {
                    renderPrefsKeysFull(modelPkg, prefsKeysObject, scalarInner = "", snapshotInner = chunk)
                } else {
                    var text = file.readText()
                    if (KmpPreferencesBlockMerger.hasConstName(text, upper)) {
                        if (force) {
                            text = replaceSnapshotKeyLine(text, upper, chunk)
                        } else {
                            throw IllegalStateException(
                                "Snapshot key const $upper already exists in ${file.name}. Use --force to replace, " +
                                    "or choose another --key.",
                            )
                        }
                    } else {
                        text = KmpPreferencesBlockMerger.appendToMarkedBlock(
                            text,
                            KmpPreferencesBlockMerger.SNAPSHOT_KEYS_BEGIN,
                            KmpPreferencesBlockMerger.SNAPSHOT_KEYS_END,
                            indentPrefsKeysBlock(chunk),
                            shouldSkipAppend = { false },
                        )
                    }
                    text
                }
            }
        }
    }

    private fun replaceScalarKeyConst(text: String, upper: String, newChunk: String): String {
        val pattern = Regex(
            """const val $upper = \"[^\"]*\"\s*\n\s*const val ${upper}_DEFAULT = [^\n]+""",
            RegexOption.MULTILINE,
        )
        return pattern.replace(text) {
            newChunk.prependIndent("        ")
        }
    }

    private fun replaceSnapshotKeyLine(text: String, upper: String, newLine: String): String {
        val pattern = Regex("""const val $upper = \"[^\"]*\"""")
        return pattern.replace(text, newLine.prependIndent("        "))
    }

    private fun renderPrefsKeysFull(
        modelPkg: String,
        prefsKeysObject: String,
        scalarInner: String,
        snapshotInner: String,
    ): String {
        val scalarBlock = if (scalarInner.isBlank()) "" else indentPrefsKeysBlock(scalarInner)
        val snapshotBlock = if (snapshotInner.isBlank()) "" else indentPrefsKeysBlock(snapshotInner)
        return buildString {
            appendLine("/*")
            appendLine(" * Copyright 2026 Mifos Initiative")
            appendLine(" *")
            appendLine(" * SPDX-License-Identifier: MPL-2.0")
            appendLine(" */")
            appendLine("package $modelPkg")
            appendLine()
            appendLine("internal object $prefsKeysObject {")
            appendLine("    object Scalar {")
            appendLine("        ${KmpPreferencesBlockMerger.SCALAR_KEYS_BEGIN}")
            if (scalarBlock.isNotBlank()) {
                appendLine(scalarBlock)
            }
            appendLine("        ${KmpPreferencesBlockMerger.SCALAR_KEYS_END}")
            appendLine("    }")
            appendLine()
            appendLine("    object Snapshot {")
            appendLine("        ${KmpPreferencesBlockMerger.SNAPSHOT_KEYS_BEGIN}")
            if (snapshotBlock.isNotBlank()) {
                appendLine(snapshotBlock)
            }
            appendLine("        ${KmpPreferencesBlockMerger.SNAPSHOT_KEYS_END}")
            appendLine("    }")
            appendLine("}")
        }.trimEnd() + "\n"
    }

    @Suppress("LongParameterList")
    private fun mergeOrCreateDataSource(
        existingFile: File,
        prefsPkg: String,
        modelPkg: String,
        dataSourceClass: String,
        prefsKeysObject: String,
        prefsKeysFq: String,
        mode: PrefsGenerationMode,
        force: Boolean,
        coreBase: String = "template.core.base",
    ): String {
        val keysImport = "import $prefsKeysFq"
val storeImport = "import ${coreBase}.preferences.TypedPreferenceStore"
        val flowImport = "import kotlinx.coroutines.flow.Flow"

        val chunk = when (mode) {
            is PrefsGenerationMode.Scalar ->
                KmpPreferencesKotlinEmitter.scalarDataSourceMethods(prefsKeysObject, mode.logicalKey, mode.field)
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesKotlinEmitter.snapshotDataSourceMethods(
                    prefsKeysObject,
                    mode.snapshotClassName,
                    mode.logicalKey,
                )
        }

        if (!existingFile.exists()) {
            val snapImport = if (mode is PrefsGenerationMode.Snapshot) {
                "import $modelPkg.${mode.snapshotClassName}"
            } else {
                ""
            }
            val scalarBody = if (mode is PrefsGenerationMode.Scalar) chunk else ""
            val snapshotBody = if (mode is PrefsGenerationMode.Snapshot) chunk else ""
            return buildString {
                appendLine("/*")
                appendLine(" * Copyright 2026 Mifos Initiative")
                appendLine(" *")
                appendLine(" * SPDX-License-Identifier: MPL-2.0")
                appendLine(" */")
                appendLine("package $prefsPkg")
                appendLine()
                appendLine(flowImport)
                appendLine(keysImport)
                if (snapImport.isNotBlank()) appendLine(snapImport)
                appendLine(storeImport)
                appendLine()
                appendLine("internal class $dataSourceClass(")
                appendLine("    private val store: TypedPreferenceStore,")
                appendLine(") {")
                appendLine()
                appendLine("    ${KmpPreferencesBlockMerger.SCALAR_DATASOURCE_BEGIN}")
                if (scalarBody.isNotBlank()) {
                    append(indentClassMemberBlock(scalarBody))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.SCALAR_DATASOURCE_END}")
                appendLine()
                appendLine("    ${KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_BEGIN}")
                if (snapshotBody.isNotBlank()) {
                    append(indentClassMemberBlock(snapshotBody))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_END}")
                appendLine("}")
            }.trimEnd() + "\n"
        }

        var text = existingFile.readText()
        val markerPair = when (mode) {
            is PrefsGenerationMode.Scalar ->
                KmpPreferencesBlockMerger.SCALAR_DATASOURCE_BEGIN to KmpPreferencesBlockMerger.SCALAR_DATASOURCE_END
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_BEGIN to KmpPreferencesBlockMerger.SNAPSHOT_DATASOURCE_END
        }
        val methodPrefix = when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val p = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                "suspend fun get$p"
            }
            is PrefsGenerationMode.Snapshot -> "suspend fun get${mode.snapshotClassName}"
        }
        if (text.contains(methodPrefix) && !force) {
            logger.info("DataSource already contains {}; skipping datasource merge", methodPrefix)
            return text
        }
        if (mode is PrefsGenerationMode.Snapshot) {
            val snapImportLine = "import $modelPkg.${mode.snapshotClassName}"
            if (!text.contains(snapImportLine)) {
                text = insertImportAfterPackage(text, snapImportLine)
            }
        }
        text = KmpPreferencesBlockMerger.appendToMarkedBlock(
            text,
            markerPair.first,
            markerPair.second,
            indentClassMemberBlock(chunk),
            shouldSkipAppend = { inner -> inner.contains(methodPrefix) },
        )
        if (!text.contains(keysImport)) {
            text = insertImportAfterPackage(text, keysImport)
        }
        if (!text.contains(storeImport)) {
            text = insertImportAfterPackage(text, storeImport)
        }
        if (!text.contains(flowImport)) {
            text = insertImportAfterPackage(text, flowImport)
        }
        return text
    }

    private fun insertImportAfterPackage(text: String, importLine: String): String {
        val pkg = Regex("^package\\s+\\S+", RegexOption.MULTILINE).find(text) ?: return importLine + "\n" + text
        val insertAt = pkg.range.last + 1
        return text.substring(0, insertAt) + "\n" + importLine + text.substring(insertAt)
    }

    private fun mergeOrCreatePrefsRepository(
        file: File,
        domainRepoPkg: String,
        prefsRepoName: String,
        mode: PrefsGenerationMode,
    ): String {
        val chunk = when (mode) {
            is PrefsGenerationMode.Scalar -> KmpPreferencesKotlinEmitter.scalarRepositoryMethods(mode.field)
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesKotlinEmitter.snapshotRepositoryMethods(mode.snapshotClassName)
        }
        if (!file.exists()) {
            return buildString {
                appendLine("/*")
                appendLine(" * Copyright 2026 Mifos Initiative")
                appendLine(" *")
                appendLine(" * SPDX-License-Identifier: MPL-2.0")
                appendLine(" */")
                appendLine("package $domainRepoPkg")
                appendLine()
                appendLine("import kotlinx.coroutines.flow.Flow")
                appendLine()
                appendLine("internal interface $prefsRepoName {")
                appendLine("    ${KmpPreferencesBlockMerger.REPOSITORY_BEGIN}")
                if (chunk.isNotBlank()) {
                    append(indentClassMemberBlock(chunk))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.REPOSITORY_END}")
                appendLine("}")
            }.trimEnd() + "\n"
        }
        val text = file.readText()
        val sig = when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val p = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                "suspend fun get$p"
            }
            is PrefsGenerationMode.Snapshot -> "suspend fun get${mode.snapshotClassName}"
        }
        return KmpPreferencesBlockMerger.appendToMarkedBlock(
            text,
            KmpPreferencesBlockMerger.REPOSITORY_BEGIN,
            KmpPreferencesBlockMerger.REPOSITORY_END,
            indentClassMemberBlock(chunk),
            shouldSkipAppend = { inner -> inner.contains(sig) },
        )
    }

    private fun mergeOrCreatePrefsSupport(
        file: File,
        dataRepoPkg: String,
        domainRepoPkg: String,
        prefsSupportName: String,
        prefsRepoName: String,
        dataSourceClass: String,
        prefsPkg: String,
        mode: PrefsGenerationMode,
    ): String {
        val chunk = when (mode) {
            is PrefsGenerationMode.Scalar ->
                KmpPreferencesKotlinEmitter.scalarSupportDelegates(mode.field, dataSourceClass)
            is PrefsGenerationMode.Snapshot ->
                KmpPreferencesKotlinEmitter.snapshotSupportDelegates(mode.snapshotClassName, dataSourceClass)
        }
        if (!file.exists()) {
            return buildString {
                appendLine("/*")
                appendLine(" * Copyright 2026 Mifos Initiative")
                appendLine(" *")
                appendLine(" * SPDX-License-Identifier: MPL-2.0")
                appendLine(" */")
                appendLine("package $dataRepoPkg")
                appendLine()
                appendLine("import $prefsPkg.$dataSourceClass")
                appendLine("import $domainRepoPkg.$prefsRepoName")
                appendLine()
                appendLine("internal class $prefsSupportName(")
                appendLine("    private val prefs: $dataSourceClass,")
                appendLine(") : $prefsRepoName {")
                appendLine("    ${KmpPreferencesBlockMerger.SUPPORT_BEGIN}")
                if (chunk.isNotBlank()) {
                    append(indentClassMemberBlock(chunk))
                    append('\n')
                }
                appendLine("    ${KmpPreferencesBlockMerger.SUPPORT_END}")
                appendLine("}")
            }.trimEnd() + "\n"
        }
        val text = file.readText()
        val sig = when (mode) {
            is PrefsGenerationMode.Scalar -> {
                val p = KmpPreferencesKotlinEmitter.kotlinPropertyToPascal(mode.field.name)
                "override suspend fun get$p"
            }
            is PrefsGenerationMode.Snapshot -> "override suspend fun get${mode.snapshotClassName}"
        }
        return KmpPreferencesBlockMerger.appendToMarkedBlock(
            text,
            KmpPreferencesBlockMerger.SUPPORT_BEGIN,
            KmpPreferencesBlockMerger.SUPPORT_END,
            indentClassMemberBlock(chunk),
            shouldSkipAppend = { inner -> inner.contains(sig) },
        )
    }

    data class KmpPreferencesScaffoldResult(
        val moduleName: String,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
