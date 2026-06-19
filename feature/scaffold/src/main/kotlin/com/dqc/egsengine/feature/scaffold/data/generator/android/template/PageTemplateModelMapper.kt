/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.data.generator.common.PagePagingDetector
import com.dqc.egsengine.feature.scaffold.data.generator.common.buildPagedUseCaseArgumentList
import com.dqc.egsengine.feature.scaffold.data.generator.common.collectContractImportsForType
import com.dqc.egsengine.feature.scaffold.data.generator.common.contractShortTypeDisplay
import com.dqc.egsengine.feature.scaffold.data.generator.common.contractShortTypeDisplayInner
import com.dqc.egsengine.feature.scaffold.data.generator.common.defaultLiteralForUseCaseParamType
import com.dqc.egsengine.feature.scaffold.data.generator.common.extractResultInnerType
import com.dqc.egsengine.feature.scaffold.data.generator.common.isDirectReturnToStateUseCase
import com.dqc.egsengine.feature.scaffold.data.generator.common.isUnitParamEchoUseCase
import com.dqc.egsengine.feature.scaffold.data.generator.common.resolvePagedItemFqn
import com.dqc.egsengine.feature.scaffold.data.generator.common.resolveParamTypeString
import com.dqc.egsengine.feature.scaffold.data.generator.common.resolveStatePropertyTypeString
import com.dqc.egsengine.feature.scaffold.data.generator.common.shouldEmitStateFieldForReturnType
import com.dqc.egsengine.feature.scaffold.data.generator.common.unitEchoStatePropertyNameFor
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import com.dqc.egsengine.feature.templateengine.model.BaseClassPackagesModel
import com.dqc.egsengine.feature.templateengine.model.PageIntentInnerModel
import com.dqc.egsengine.feature.templateengine.model.PageStateFieldModel
import com.dqc.egsengine.feature.templateengine.model.PageTemplateModel
import com.dqc.egsengine.feature.templateengine.model.PageUseCaseHandlerModel
import com.dqc.egsengine.feature.templateengine.model.PageUseCaseModel
import com.dqc.egsengine.feature.templateengine.model.PageUseCaseParamModel

internal fun PageTemplate.toPageTemplateModel(): PageTemplateModel {
    val pascalName = pageName
    val camelName = pascalName.replaceFirstChar { it.lowercase() }
    val layoutSnakeName = pascalName.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    val screenPkg = "$modulePackage.presentation.screen.$camelName"
    val screenDirPkg = screenPkg

    /** Swagger / API-sync generated DTOs (same as [KmpPageTemplateModelMapper]). */
    val modelPackage = "$modulePackage.generate.domain.model"
    val resultPackage = basePackage?.let { "$it.feature.base.domain.result" } ?: "com.example.feature.base.domain.result"
    val uiContractPackage =
        baseClassPackages.baseViewModel?.substringBeforeLast(".")
            ?: basePackage?.let { "$it.feature.base.presentation.viewmodel" }
            ?: "com.example.feature.base.presentation.viewmodel"

    val hasBaseViewModel = baseClassPackages.baseViewModel != null
    val baseVmFqcn = baseClassPackages.baseViewModel
    val baseVmSimple = baseVmFqcn?.substringAfterLast(".")
    val baseViewModelIsAndroidX = !hasBaseViewModel

    val pagingOpt = PagePagingDetector.normalizePagingOption(pagingOption)
    val hasPagedOffset = useCases.any { PagePagingDetector.isOffsetPageResultUseCase(it.returnType, pagingOpt, it.parameters) }
    val hasPagedFlow = useCases.any { PagePagingDetector.isPaging3FlowUseCase(it.returnType, pagingOpt) }
    val primaryOffsetUc = useCases.firstOrNull { PagePagingDetector.isOffsetPageResultUseCase(it.returnType, pagingOpt, it.parameters) }
    val primaryFlowUc = useCases.firstOrNull { PagePagingDetector.isPaging3FlowUseCase(it.returnType, pagingOpt) }

    val primaryPagedArgList: String =
        if (primaryOffsetUc != null) {
            val h = PagePagingDetector.detectPageParams(primaryOffsetUc.parameters)
            buildPagedUseCaseArgumentList(primaryOffsetUc, h.page, h.pageSize)
        } else {
            ""
        }

    val primaryPagedNonPageArgList: String =
        if (primaryOffsetUc != null) {
            val h = PagePagingDetector.detectPageParams(primaryOffsetUc.parameters)
            primaryOffsetUc.parameters
                .filter { it.name != h.page && it.name != h.pageSize }
                .joinToString(", ") { p -> "${p.name} = ${defaultLiteralForUseCaseParamType(p.type)}" }
        } else {
            ""
        }

    var pagedItemFqn = ""
    var pagedItemContractRef = ""
    var pagedConcreteInnerShort = ""
    if (primaryOffsetUc != null) {
        val inner = extractResultInnerType(primaryOffsetUc.returnType.orEmpty()).orEmpty()
        pagedConcreteInnerShort = contractShortTypeDisplayInner(inner)
        pagedItemFqn = resolvePagedItemFqn(inner, modelPackage, modulePackage) { extractResultInnerType(it) }
        pagedItemContractRef = contractShortTypeDisplay(pagedItemFqn)
    }
    var flowPagedItemContractRef = ""
    if (primaryFlowUc != null) {
        val raw = PagePagingDetector.extractPagingDataItemType(primaryFlowUc.returnType.orEmpty()) ?: ""
        val fqn = resolveParamTypeString(raw, modelPackage, modulePackage)
        flowPagedItemContractRef = contractShortTypeDisplay(fqn)
    }

    val useCaseModels = useCases.map { it.toPageUseCaseModel(modelPackage, modulePackage) }

    val stateFields =
        buildList {
            useCases.forEach { uc ->
                val rt = uc.returnType ?: return@forEach
                if (PagePagingDetector.isOffsetPageResultUseCase(rt, pagingOpt, uc.parameters)) return@forEach
                if (PagePagingDetector.isPaging3FlowUseCase(rt, pagingOpt)) return@forEach
                if (!shouldEmitStateFieldForReturnType(rt) { extractResultInnerType(it) }) return@forEach
                val propName = uc.camelName
                val typeFqn = resolveStatePropertyTypeString(rt, modelPackage, modulePackage)
                add(
                    PageStateFieldModel(
                        name = propName,
                        typeFqn = typeFqn,
                        typeContractRef = contractShortTypeDisplay(typeFqn),
                        nullable = true,
                        defaultLiteral = null,
                    ),
                )
            }
            if (hasPagedOffset && primaryOffsetUc != null && pagedItemFqn.isNotBlank()) {
                add(
                    PageStateFieldModel(
                        name = "items",
                        typeFqn = "List<$pagedItemFqn>",
                        typeContractRef = "List<$pagedItemContractRef>",
                        nullable = false,
                        defaultLiteral = "emptyList()",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "total",
                        typeFqn = "Long",
                        typeContractRef = "Long",
                        nullable = false,
                        defaultLiteral = "0L",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "page",
                        typeFqn = "Int",
                        typeContractRef = "Int",
                        nullable = false,
                        defaultLiteral = "DEFAULT_FIRST_PAGE",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "pageSize",
                        typeFqn = "Int",
                        typeContractRef = "Int",
                        nullable = false,
                        defaultLiteral = "DEFAULT_PAGE_SIZE",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "isRefreshing",
                        typeFqn = "Boolean",
                        typeContractRef = "Boolean",
                        nullable = false,
                        defaultLiteral = "false",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "isLoadingMore",
                        typeFqn = "Boolean",
                        typeContractRef = "Boolean",
                        nullable = false,
                        defaultLiteral = "false",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "endReached",
                        typeFqn = "Boolean",
                        typeContractRef = "Boolean",
                        nullable = false,
                        defaultLiteral = "false",
                    ),
                )
                add(
                    PageStateFieldModel(
                        name = "pagingError",
                        typeFqn = "Throwable",
                        typeContractRef = "Throwable",
                        nullable = true,
                        defaultLiteral = null,
                    ),
                )
            }
            useCases.forEach { uc ->
                if (!isUnitParamEchoUseCase(uc, modelPackage, modulePackage)) return@forEach
                val paramFqn =
                    resolveParamTypeString(uc.parameters.single().type, modelPackage, modulePackage)
                val propName = unitEchoStatePropertyNameFor(uc, paramFqn)
                add(
                    PageStateFieldModel(
                        name = propName,
                        typeFqn = paramFqn,
                        typeContractRef = contractShortTypeDisplay(paramFqn),
                        nullable = true,
                        defaultLiteral = null,
                    ),
                )
            }
        }

    val intentInners =
        buildList {
            if (hasPagedOffset) {
                add(PageIntentInnerModel("Refresh", true, emptyList()))
                add(PageIntentInnerModel("LoadMore", true, emptyList()))
                add(PageIntentInnerModel("Retry", true, emptyList()))
            }
            useCases.filterNot { PagePagingDetector.isOffsetPageResultUseCase(it.returnType, pagingOpt, it.parameters) }.forEach { uc ->
                val intentName = uc.name.removeSuffix("UseCase")
                val emptyParams = uc.parameters.isEmpty()
                add(
                    PageIntentInnerModel(
                        simpleName = intentName,
                        emptyParams = emptyParams,
                        params = uc.parameters.map { it.toPageParamModel(modelPackage, modulePackage) },
                    ),
                )
            }
        }

    val contractImports = buildContractImports(stateFields, intentInners, hasPagedOffset, uiContractPackage)

    val useCaseHandlers =
        useCases.map { uc ->
            val intentName = uc.name.removeSuffix("UseCase")
            val rt = uc.returnType.orEmpty()
            val offsetPaged = PagePagingDetector.isOffsetPageResultUseCase(rt, pagingOpt, uc.parameters)
            val flowPaged = PagePagingDetector.isPaging3FlowUseCase(rt, pagingOpt)
            val pageParams = PagePagingDetector.detectPageParams(uc.parameters)
            val flowBasedRaw = looksLikeFlowReturn(rt)
            val unitEcho = isUnitParamEchoUseCase(uc, modelPackage, modulePackage)
            val direct = isDirectReturnToStateUseCase(uc, modelPackage, modulePackage)
            val flowBased = !unitEcho && !direct && flowBasedRaw && !flowPaged
            val resultBased =
                !offsetPaged && !flowPaged && !unitEcho && !direct && !flowBasedRaw && looksLikeResultReturn(rt)
            val echoProp =
                if (unitEcho) {
                    unitEchoStatePropertyNameFor(
                        uc,
                        resolveParamTypeString(uc.parameters.single().type, modelPackage, modulePackage),
                    )
                } else {
                    ""
                }
            val echoParamName = if (unitEcho) uc.parameters.single().name else ""
            val innerRt = extractResultInnerType(rt)
            val itemRaw =
                innerRt?.let {
                    when {
                        PagePagingDetector.isGenericPageResultType(it) -> PagePagingDetector.extractPageResultItemRaw(it)
                        PagePagingDetector.isConcretePageResultInner(it) -> PagePagingDetector.extractConcretePageResultItemSimpleName(it)
                        else -> null
                    }
                } ?: ""
            val pagedItemFqnForUc =
                if (itemRaw.isNotBlank()) {
                    resolveParamTypeString(itemRaw, modelPackage, modulePackage)
                } else {
                    ""
                }
            val flowItemRaw = PagePagingDetector.extractPagingDataItemType(rt) ?: ""
            val flowItemFqnForUc =
                if (flowItemRaw.isNotBlank()) resolveParamTypeString(flowItemRaw, modelPackage, modulePackage) else ""
            PageUseCaseHandlerModel(
                intentSimpleName = intentName,
                handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}",
                useCaseCamel = uc.camelName,
                hasParams = uc.parameters.isNotEmpty(),
                paramPassArgs = uc.parameters.joinToString(", ") { "${it.name} = ${it.name}" },
                showLoading = !(uc.returnType?.contains("SseEmitter") == true),
                resultBased = resultBased,
                flowBased = flowBased,
                unitEntityEchoToState = unitEcho,
                unitEchoStatePropertyName = echoProp,
                unitEchoParamName = echoParamName,
                directReturnToState = direct,
                directStatePropertyName = if (direct) uc.camelName else "",
                pagedBased = offsetPaged,
                pagedFlowBased = flowPaged,
                pageParam = pageParams.page,
                pageSizeParam = pageParams.pageSize,
                pagedItemTypeFqn = pagedItemFqnForUc,
                pagedItemTypeContractRef =
                if (pagedItemFqnForUc.isNotBlank()) {
                    contractShortTypeDisplay(pagedItemFqnForUc)
                } else {
                    ""
                },
                flowPagedItemTypeFqn = flowItemFqnForUc,
                flowPagedItemTypeContractRef =
                if (flowItemFqnForUc.isNotBlank()) {
                    contractShortTypeDisplay(flowItemFqnForUc)
                } else {
                    ""
                },
            )
        }
    val hasResultBasedHandler = useCaseHandlers.any { it.resultBased || it.pagedBased } || hasPagedOffset

    return PageTemplateModel(
        pascalName = pascalName,
        camelName = camelName,
        layoutSnakeName = layoutSnakeName,
        modulePackage = modulePackage,
        screenPkg = screenPkg,
        screenDirPkg = screenDirPkg,
        modelPackage = modelPackage,
        resultPackage = resultPackage,
        uiContractPackage = uiContractPackage,
        baseClasses =
        BaseClassPackagesModel(
            baseViewModel = baseClassPackages.baseViewModel,
            baseFragment = baseClassPackages.baseFragment,
            resultClass = baseClassPackages.resultClass,
            retrofitProvider = baseClassPackages.retrofitProvider,
        ),
        basePackage = basePackage,
        useCases = useCaseModels,
        hasUseCases = useCases.isNotEmpty(),
        hasBaseViewModel = hasBaseViewModel,
        baseViewModelIsAndroidX = baseViewModelIsAndroidX,
        baseViewModelImport = baseVmFqcn,
        baseViewModelSimpleName = baseVmSimple,
        stateFields = stateFields,
        contractImports = contractImports,
        intentInners = intentInners,
        useCaseHandlers = useCaseHandlers,
        hasResultBasedHandler = hasResultBasedHandler,
        pagingOption = pagingOpt,
        hasPagedOffset = hasPagedOffset,
        hasPagedFlow = hasPagedFlow,
        pagedItemTypeContractRef = pagedItemContractRef,
        pagedItemTypeFqn = pagedItemFqn,
        flowPagedItemTypeContractRef = flowPagedItemContractRef,
        defaultPageSize = 20,
        primaryPagedArgList = primaryPagedArgList,
        primaryPagedUseCaseCamel = primaryOffsetUc?.camelName ?: "",
        pagingListStateInterfaceFqn = "$uiContractPackage.PagingListState",
        pageResultClassFqn = "$uiContractPackage.PageResult",
        pagedStateItemContractRef = pagedItemContractRef,
        pagedConcreteInnerContractRef = pagedConcreteInnerShort,
        primaryPagedNonPageArgList = primaryPagedNonPageArgList,
    )
}

private fun looksLikeFlowReturn(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    if (returnType.contains("kotlinx.coroutines.flow")) return true
    return FLOW_TYPE_REGEX.containsMatchIn(returnType)
}

private val FLOW_TYPE_REGEX =
    Regex("""\b(Flow|StateFlow|SharedFlow|MutableStateFlow|MutableSharedFlow)\s*<""")

private fun looksLikeResultReturn(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    return returnType.contains("Result<") ||
        returnType.contains(".Result<") ||
        returnType.contains("domain.result.Result<") ||
        returnType.contains("generate.domain.result.Result<") ||
        returnType.contains("base.domain.result.Result<") ||
        returnType.endsWith(".Result") ||
        returnType.contains("network.domain.Result")
}

private fun UseCaseParam.toPageParamModel(
    modelPackage: String,
    modulePackage: String,
): PageUseCaseParamModel {
    val fqn = resolveParamTypeString(type, modelPackage, modulePackage)
    return PageUseCaseParamModel(
        name = name,
        type = type,
        kotlinType = fqn,
        kotlinTypeContractRef = contractShortTypeDisplay(fqn),
        placeholderValue = placeholderValue,
    )
}

private fun UseCaseInfo.toPageUseCaseModel(
    modelPackage: String,
    modulePackage: String,
): PageUseCaseModel {
    val intentName = name.removeSuffix("UseCase")
    return PageUseCaseModel(
        name = name,
        camelName = camelName,
        packageName = packageName,
        returnType = returnType,
        parameters = parameters.map { it.toPageParamModel(modelPackage, modulePackage) },
        intentName = intentName,
        handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}",
    )
}

private fun buildContractImports(
    stateFields: List<PageStateFieldModel>,
    intentInners: List<PageIntentInnerModel>,
    hasPagedOffset: Boolean,
    uiContractPackage: String,
): List<String> {
    val out = mutableSetOf<String>()
    stateFields.forEach { f ->
        collectContractImportsForType(f.typeFqn, out)
    }
    intentInners.flatMap { it.params }.forEach { p ->
        collectContractImportsForType(p.kotlinType, out)
    }
    if (hasPagedOffset) {
        out.add("import $uiContractPackage.DEFAULT_FIRST_PAGE")
        out.add("import $uiContractPackage.DEFAULT_PAGE_SIZE")
        out.add("import $uiContractPackage.PagingListState")
    }
    return out.sorted()
}

internal fun buildParamTypeForTemplate(
    param: com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam,
    modelPackage: String,
    modulePackage: String,
): String = resolveParamTypeString(param.type, modelPackage, modulePackage)
