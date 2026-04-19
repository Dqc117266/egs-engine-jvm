/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.buildAndroidMergeSnippetForUseCase
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.toPageTemplateModel
import com.dqc.egsengine.feature.scaffold.data.generator.common.PagePagingDetector
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.PageFileLocator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.buildMergeSnippetForUseCase
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.toKmpPageTemplateMap
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.ViewModelMemberMerger
import com.dqc.egsengine.feature.scaffold.data.kotlin.KotlinMemberInspector
import com.dqc.egsengine.feature.scaffold.domain.model.GeneratedFileInfo
import com.dqc.egsengine.feature.scaffold.domain.model.PageScaffoldResult
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.domain.effectiveBasePackage
import com.dqc.egsengine.feature.scaffold.domain.resolveScaffoldBaseClasses
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Idempotently wires additional use cases into an existing Contract / ViewModel / PresentationModule
 * (KMP `commonMain/kotlin` or Android `main/kotlin`, `presentation/screen` or `presentation/fragment`).
 */
class ViewModelEditScaffolder(
    private val configReader: EgsConfigReader,
    private val useCaseScanner: UseCaseScanner,
    private val featureDiUpdater: FeatureDiUpdater,
) {
    private val logger = LoggerFactory.getLogger(ViewModelEditScaffolder::class.java)

    data class EditStats(
        val ctorParams: Int,
        val intents: Int,
        val stateFields: Int,
        val registerBlocks: Int,
        val handlers: Int,
        val contractImports: Int,
        val viewModelImports: Int,
    )

    data class ViewModelEditResult(
        val pageScaffoldResult: PageScaffoldResult,
        val stats: EditStats,
        val diffs: Map<String, String>,
    )

    fun edit(
        projectRoot: File,
        moduleName: String,
        pageName: String,
        selectedUseCases: List<UseCaseInfo>,
        dryRun: Boolean,
        workspaceRoot: File = projectRoot,
        pagingOption: String = "auto",
    ): ViewModelEditResult {
        logger.info("Edit viewmodel '$pageName' in module '$moduleName'")

        val clientRoot = projectRoot
        if (selectedUseCases.isEmpty()) {
            return emptyResult(pageName, moduleName, dryRun)
        }

        val enriched = useCaseScanner.enrichReturnTypesIfMissing(clientRoot, selectedUseCases)

        val config = configReader.readForScaffold(clientRoot, workspaceRoot)
        val basePackage = config.effectiveBasePackage()
        val modulePackage =
            if (basePackage != null) {
                "$basePackage.feature.$moduleName"
            } else {
                "com.example.feature.$moduleName"
            }

        val moduleDir = moduleDir(clientRoot, moduleName)
        val baseClasses = config.resolveScaffoldBaseClasses(includeRetrofitProvider = true)
        val pageBaseClasses =
            if (useKmpPageTemplates(config, moduleDir)) {
                baseClasses.copy(resultClass = null)
            } else {
                baseClasses
            }

        val kotlinRootRel = kotlinSourceRootRelative(moduleDir)
        val useKmpTemplates = useKmpPageTemplates(config, moduleDir)

        val pascal = pageName.replaceFirstChar { it.uppercase() }
        val (contractFile, vmFile, presentationLayout) =
            locateContractAndViewModel(clientRoot, moduleName, modulePackage, kotlinRootRel, pascal)

        val contractText = contractFile.readText()
        val vmText = vmFile.readText()

        val existingCtor = KotlinMemberInspector.parsePrimaryConstructorParams(vmText, "${pascal}ViewModel")
        val allInModule = useCaseScanner.scanByModule(clientRoot, moduleName)
        val existingUseCases =
            existingCtor.map { p ->
                val short = p.type.trim().substringAfterLast(".")
                allInModule.find { it.name == short }
                    ?: throw IllegalArgumentException(
                        "ViewModel ctor references '$short' but no matching UseCase was found in module '$moduleName'",
                    )
            }
        val existingTypes = existingUseCases.map { it.name }.toSet()

        val existingUcNames = existingUseCases.map { it.name }.toSet()
        val actuallyToAdd = enriched.filter { it.name !in existingUcNames }

        if (actuallyToAdd.isEmpty()) {
            logger.info("Nothing to add — all selected use cases already in ctor")
            return emptyResult(pageName, moduleName, dryRun)
        }

        val mergedUseCases = (existingUseCases + actuallyToAdd).distinctBy { it.name }

        val template = PageTemplate(
            pageName = pascal,
            moduleName = moduleName,
            modulePackage = modulePackage,
            useCases = mergedUseCases,
            basePackage = basePackage,
            baseClassPackages = pageBaseClasses,
            pagingOption = pagingOption,
        )

        val existingImports = parseImportLineSet(contractText)

        val fullContractImports: List<String>
        val snippets =
            if (useKmpTemplates) {
                val templateMap = template.toKmpPageTemplateMap()
                validatePagingIfNeeded(templateMap, contractText, pagingOption)
                @Suppress("UNCHECKED_CAST")
                fullContractImports = templateMap["contractImports"] as List<String>
                actuallyToAdd.map { add ->
                    val idx = mergedUseCases.indexOfFirst { it.name == add.name }
                    require(idx >= 0) { "merged use case list missing ${add.name}" }
                    buildMergeSnippetForUseCase(template, templateMap, idx)
                }
            } else {
                validateAndroidViewModelForMerge(vmText)
                val pageModel = template.toPageTemplateModel()
                fullContractImports = pageModel.contractImports
                actuallyToAdd.map { add ->
                    val idx = mergedUseCases.indexOfFirst { it.name == add.name }
                    require(idx >= 0) { "merged use case list missing ${add.name}" }
                    buildAndroidMergeSnippetForUseCase(template, pageModel, idx)
                }
            }

        val newContractImportLines =
            if (snippets.any { it.stateFieldText != null }) {
                fullContractImports.filter { it !in existingImports }.sorted()
            } else {
                emptyList()
            }

        val intentNames = KotlinMemberInspector.sealedIntentMemberNames(contractText)
        val stateNames = KotlinMemberInspector.dataClassPropertyNames(contractText)

        val contractMerge =
            ViewModelMemberMerger.mergeContract(
                contractText = contractText,
                snippets = snippets,
                existingIntentNames = intentNames,
                existingStateNames = stateNames,
                newContractImportLines = newContractImportLines,
            )

        val registerNames =
            KotlinMemberInspector.registerIntentBranchNames(contractText, pascal)
        val handlerNames = KotlinMemberInspector.privateHandlerFunctionNames(vmText)

        val vmMerge =
            ViewModelMemberMerger.mergeViewModel(
                vmText = vmText,
                pascalName = pascal,
                snippets = snippets,
                existingCtorTypes = existingTypes,
                existingRegisterBranches = registerNames,
                existingHandlerNames = handlerNames,
            )

        val stats =
            EditStats(
                ctorParams = vmMerge.stats.ctorParams,
                intents = contractMerge.stats.intents,
                stateFields = contractMerge.stats.stateFields,
                registerBlocks = vmMerge.stats.registerBlocks,
                handlers = vmMerge.stats.handlers,
                contractImports = contractMerge.stats.contractImports,
                viewModelImports = vmMerge.stats.viewModelImports,
            )

        val relContract = contractFile.relativeTo(clientRoot).path
        val relVm = vmFile.relativeTo(clientRoot).path

        val diffs = mutableMapOf<String, String>()
        if (contractMerge.text != contractText) {
            diffs[relContract] = TextDiffUtil.unifiedDiff(contractText, contractMerge.text, contractFile.name)
        }
        if (vmMerge.text != vmText) {
            diffs[relVm] = TextDiffUtil.unifiedDiff(vmText, vmMerge.text, vmFile.name)
        }

        val filesOut = mutableListOf<GeneratedFileInfo>()
        if (!dryRun) {
            if (contractMerge.text != contractText) {
                contractFile.writeText(contractMerge.text)
                filesOut.add(GeneratedFileInfo(relContract, contractMerge.text))
            }
            if (vmMerge.text != vmText) {
                vmFile.writeText(vmMerge.text)
                filesOut.add(GeneratedFileInfo(relVm, vmMerge.text))
            }
            if (filesOut.isNotEmpty()) {
                featureDiUpdater.updatePresentationModule(
                    projectRoot = clientRoot,
                    moduleName = moduleName,
                    modulePackage = modulePackage,
                    pageName = pascal,
                    useCases = mergedUseCases,
                    kotlinRootRel = kotlinRootRel,
                    useScreenPresentationLayout = presentationLayout == PageFileLocator.PresentationLayout.Screen,
                )
            }
        } else {
            if (contractMerge.text != contractText) {
                filesOut.add(GeneratedFileInfo(relContract, contractMerge.text))
            }
            if (vmMerge.text != vmText) {
                filesOut.add(GeneratedFileInfo(relVm, vmMerge.text))
            }
        }

        return ViewModelEditResult(
            pageScaffoldResult =
                PageScaffoldResult(
                    pageName = pascal,
                    moduleName = moduleName,
                    files = filesOut,
                    dryRun = dryRun,
                ),
            stats = stats,
            diffs = diffs,
        )
    }

    private fun emptyResult(pageName: String, moduleName: String, dryRun: Boolean) =
        ViewModelEditResult(
            pageScaffoldResult =
                PageScaffoldResult(
                    pageName = pageName.replaceFirstChar { it.uppercase() },
                    moduleName = moduleName,
                    files = emptyList(),
                    dryRun = dryRun,
                ),
            stats =
                EditStats(0, 0, 0, 0, 0, 0, 0),
            diffs = emptyMap(),
        )

    private fun validatePagingIfNeeded(
        templateMap: Map<String, Any?>,
        contractText: String,
        pagingOption: String,
    ) {
        if (templateMap["hasPagedOffset"] != true) return
        if (PagePagingDetector.normalizePagingOption(pagingOption) == "none") return
        val props = KotlinMemberInspector.dataClassPropertyNames(contractText)
        if (!props.contains("items") || !props.contains("page")) {
            throw IllegalArgumentException(
                "Paging (offset) requires State fields items/page. " +
                    "Run `egs create screen ... --paging offset` first, or pass `--paging none` to skip paging intents.",
            )
        }
    }

    private fun moduleDir(root: File, moduleName: String) = root.resolve("feature/$moduleName")

    private fun kotlinSourceRootRelative(moduleDir: File): String =
        when {
            moduleDir.resolve("src/commonMain/kotlin").isDirectory -> "src/commonMain/kotlin"
            else -> "src/main/kotlin"
        }

    private fun useKmpPageTemplates(config: EgsConfig, moduleDir: File): Boolean {
        val t = config.projectType.uppercase()
        if (t in setOf("KMP", "KMP_ANDROID")) return true
        return moduleDir.resolve("src/commonMain/kotlin").isDirectory
    }

    private fun parseImportLineSet(source: String): Set<String> =
        source.lineSequence().map { it.trim() }.filter { it.startsWith("import ") }.toSet()

    private fun locateContractAndViewModel(
        clientRoot: File,
        moduleName: String,
        modulePackage: String,
        kotlinRootRel: String,
        pascal: String,
    ): Triple<File, File, PageFileLocator.PresentationLayout> {
        val screen =
            PageFileLocator.screenKotlinPaths(
                clientRoot,
                moduleName,
                modulePackage,
                kotlinRootRel,
                pascal,
                PageFileLocator.PresentationLayout.Screen,
            )
        val fragment =
            PageFileLocator.screenKotlinPaths(
                clientRoot,
                moduleName,
                modulePackage,
                kotlinRootRel,
                pascal,
                PageFileLocator.PresentationLayout.Fragment,
            )
        return when {
            screen.contract.isFile && screen.viewModel.isFile ->
                Triple(screen.contract, screen.viewModel, PageFileLocator.PresentationLayout.Screen)
            fragment.contract.isFile && fragment.viewModel.isFile ->
                Triple(fragment.contract, fragment.viewModel, PageFileLocator.PresentationLayout.Fragment)
            else -> {
                val rel = clientRoot.path
                throw IllegalArgumentException(
                    "Contract/ViewModel not found for page '$pascal'. Tried:\n" +
                        "  ${screen.contract.relativeTo(clientRoot).path}\n" +
                        "  ${fragment.contract.relativeTo(clientRoot).path}\n" +
                        "Under Gradle client root: $rel",
                )
            }
        }
    }

    private fun validateAndroidViewModelForMerge(vmText: String) {
        if (!vmText.contains("override fun registerIntents()")) {
            throw IllegalArgumentException(
                "This Android ViewModel is not in MVI scaffold form (missing registerIntents). " +
                    "Recreate the screen with `egs create screen` (with your app BaseViewModel), then run edit viewmodel again.",
            )
        }
    }
}

internal object TextDiffUtil {
    fun unifiedDiff(old: String, new: String, label: String): String {
        val a = old.lines()
        val b = new.lines()
        val sb = StringBuilder()
        sb.appendLine("--- $label (before)")
        sb.appendLine("+++ $label (after)")
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            when {
                i < a.size && j < b.size && a[i] == b[j] -> {
                    sb.appendLine(" ${a[i]}")
                    i++
                    j++
                }
                j < b.size && (i >= a.size || (i < a.size && a[i] != b[j] && !a.subList(i, a.size).contains(b[j]))) -> {
                    sb.appendLine("+${b[j]}")
                    j++
                }
                i < a.size -> {
                    sb.appendLine("-${a[i]}")
                    i++
                }
            }
        }
        return sb.toString().trimEnd()
    }
}
