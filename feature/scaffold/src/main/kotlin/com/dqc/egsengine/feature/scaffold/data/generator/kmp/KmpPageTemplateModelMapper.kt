/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.common.PagePagingDetector
import com.dqc.egsengine.feature.scaffold.data.generator.common.buildPagedUseCaseArgumentList
import com.dqc.egsengine.feature.scaffold.data.generator.common.collectContractImportsForType
import com.dqc.egsengine.feature.scaffold.data.generator.common.contractShortTypeDisplay
import com.dqc.egsengine.feature.scaffold.data.generator.common.contractShortTypeDisplayInner
import com.dqc.egsengine.feature.scaffold.data.generator.common.defaultLiteralForUseCaseParamType
import com.dqc.egsengine.feature.scaffold.data.generator.common.isDirectReturnToStateUseCase
import com.dqc.egsengine.feature.scaffold.data.generator.common.isUnitParamEchoUseCase
import com.dqc.egsengine.feature.scaffold.data.generator.common.looksLikeFlowReturn
import com.dqc.egsengine.feature.scaffold.data.generator.common.looksLikeResultReturn
import com.dqc.egsengine.feature.scaffold.data.generator.common.resolvePagedItemFqn
import com.dqc.egsengine.feature.scaffold.data.generator.common.resolveParamTypeString
import com.dqc.egsengine.feature.scaffold.data.generator.common.resolveStatePropertyTypeString
import com.dqc.egsengine.feature.scaffold.data.generator.common.shouldEmitStateFieldForReturnType
import com.dqc.egsengine.feature.scaffold.data.generator.common.unitEchoStatePropertyNameFor
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate

/**
 * Builds a FreeMarker root model for KMP page templates under `templates/kmp/page/`.
 * Package layout: [modulePackage].presentation.screen.[camelPageName] (sources under `presentation/screen/`).
 * Swagger-generated models resolve under [modulePackage].generate.domain.model; [Result] uses the shared core-network type.
 */
internal fun PageTemplate.toKmpPageTemplateMap(): Map<String, Any?> {
    val pascalName = pageName
    val camelName = pascalName.replaceFirstChar { it.lowercase() }
    val modelPackage = "$modulePackage.generate.domain.model"
    val screenPkg = "$modulePackage.presentation.screen.$camelName"
    val coreBase = basePackage?.let { "$it.core.base" } ?: "template.core.base"
    val resultClassFqn =
        baseClassPackages.resultClass ?: "$coreBase.network.domain.Result"
    val pagingOpt = PagePagingDetector.normalizePagingOption(pagingOption)

    val useCaseRows =
        useCases.map { uc ->
            val intentName = uc.name.removeSuffix("UseCase")
            val handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}"
            mapOf(
                "name" to uc.name,
                "camelName" to uc.camelName,
                "packageName" to uc.packageName,
                "returnType" to (uc.returnType ?: ""),
                "intentName" to intentName,
                "handlerName" to handlerName,
                "parameters" to
                    uc.parameters.map { p ->
                        mapOf(
                            "name" to p.name,
                            "kotlinType" to resolveParamTypeString(p.type, modelPackage, modulePackage),
                        )
                    },
            )
        }

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
        val inner = PagePagingDetector.extractResultInnerType(primaryOffsetUc.returnType.orEmpty()) ?: ""
        pagedConcreteInnerShort = contractShortTypeDisplayInner(inner)
        pagedItemFqn = resolvePagedItemFqn(inner, modelPackage, modulePackage) { PagePagingDetector.extractResultInnerType(it) }
        pagedItemContractRef = contractShortTypeDisplay(pagedItemFqn)
    }
    var flowPagedItemContractRef = ""
    if (primaryFlowUc != null) {
        val raw = PagePagingDetector.extractPagingDataItemType(primaryFlowUc.returnType.orEmpty()) ?: ""
        val fqn = resolveParamTypeString(raw, modelPackage, modulePackage)
        flowPagedItemContractRef = contractShortTypeDisplay(fqn)
    }

    val stateFields =
        buildList {
            useCases.forEach { uc ->
                val rt = uc.returnType ?: return@forEach
                if (PagePagingDetector.isOffsetPageResultUseCase(rt, pagingOpt, uc.parameters)) return@forEach
                if (PagePagingDetector.isPaging3FlowUseCase(rt, pagingOpt)) return@forEach
                if (PagePagingDetector.looksLikeFlowReturn(rt) && extractFlowInnerType(rt) != null) {
                    val inner = extractFlowInnerType(rt)!!.trim()
                    val typeStr = resolveParamTypeString(inner, modelPackage, modulePackage)
                    add(
                        mapOf(
                            "name" to uc.camelName,
                            "typeFqn" to typeStr,
                            "typeContractRef" to contractShortTypeDisplay(typeStr),
                            "nullable" to true,
                            "defaultLiteral" to null,
                        ),
                    )
                    return@forEach
                }
                if (!shouldEmitStateFieldForReturnType(rt) { PagePagingDetector.extractResultInnerType(it) }) return@forEach
                val propName = uc.camelName
                val typeStr = resolveStatePropertyTypeString(rt, modelPackage, modulePackage, fixEntity = false)
                add(
                    mapOf(
                        "name" to propName,
                        "typeFqn" to typeStr,
                        "typeContractRef" to contractShortTypeDisplay(typeStr),
                        "nullable" to true,
                        "defaultLiteral" to null,
                    ),
                )
            }
            if (hasPagedOffset && primaryOffsetUc != null && pagedItemFqn.isNotBlank()) {
                add(
                    mapOf(
                        "name" to "items",
                        "typeFqn" to "List<$pagedItemFqn>",
                        "typeContractRef" to "List<$pagedItemContractRef>",
                        "nullable" to false,
                        "defaultLiteral" to "emptyList()",
                    ),
                )
                add(
                    mapOf(
                        "name" to "page",
                        "typeFqn" to "Int",
                        "typeContractRef" to "Int",
                        "nullable" to false,
                        "defaultLiteral" to "DEFAULT_FIRST_PAGE",
                    ),
                )
                add(
                    mapOf(
                        "name" to "pageSize",
                        "typeFqn" to "Int",
                        "typeContractRef" to "Int",
                        "nullable" to false,
                        "defaultLiteral" to "DEFAULT_PAGE_SIZE",
                    ),
                )
                add(
                    mapOf(
                        "name" to "total",
                        "typeFqn" to "Long",
                        "typeContractRef" to "Long",
                        "nullable" to false,
                        "defaultLiteral" to "0L",
                    ),
                )
                add(
                    mapOf(
                        "name" to "endReached",
                        "typeFqn" to "Boolean",
                        "typeContractRef" to "Boolean",
                        "nullable" to false,
                        "defaultLiteral" to "false",
                    ),
                )
                add(
                    mapOf(
                        "name" to "isRefreshing",
                        "typeFqn" to "Boolean",
                        "typeContractRef" to "Boolean",
                        "nullable" to false,
                        "defaultLiteral" to "false",
                    ),
                )
                add(
                    mapOf(
                        "name" to "isLoadingMore",
                        "typeFqn" to "Boolean",
                        "typeContractRef" to "Boolean",
                        "nullable" to false,
                        "defaultLiteral" to "false",
                    ),
                )
                add(
                    mapOf(
                        "name" to "pagingError",
                        "typeFqn" to "Throwable",
                        "typeContractRef" to "Throwable",
                        "nullable" to true,
                        "defaultLiteral" to null,
                    ),
                )
            }
            useCases.forEach { uc ->
                if (!isUnitParamEchoUseCase(uc, modelPackage, modulePackage)) return@forEach
                val paramFqn =
                    resolveParamTypeString(uc.parameters.single().type, modelPackage, modulePackage)
                val propName = unitEchoStatePropertyNameFor(uc, paramFqn)
                add(
                    mapOf(
                        "name" to propName,
                        "typeFqn" to paramFqn,
                        "typeContractRef" to contractShortTypeDisplay(paramFqn),
                        "nullable" to true,
                        "defaultLiteral" to null,
                    ),
                )
            }
        }

    val contractImports = buildKmpContractImports(stateFields, hasPagedOffset, coreBase)

    val intentInners =
        buildList {
            if (hasPagedOffset) {
                add(
                    mapOf(
                        "simpleName" to "Refresh",
                        "emptyParams" to true,
                        "params" to emptyList<Map<String, String>>(),
                    ),
                )
                add(
                    mapOf(
                        "simpleName" to "LoadMore",
                        "emptyParams" to true,
                        "params" to emptyList<Map<String, String>>(),
                    ),
                )
                add(
                    mapOf(
                        "simpleName" to "Retry",
                        "emptyParams" to true,
                        "params" to emptyList<Map<String, String>>(),
                    ),
                )
            }
            useCases.filterNot { PagePagingDetector.isOffsetPageResultUseCase(it.returnType, pagingOpt, it.parameters) }.forEach { uc ->
                val intentName = uc.name.removeSuffix("UseCase")
                val emptyParams = uc.parameters.isEmpty()
                add(
                    mapOf(
                        "simpleName" to intentName,
                        "emptyParams" to emptyParams,
                        "params" to
                            uc.parameters.map { p ->
                                mapOf(
                                    "name" to p.name,
                                    "kotlinType" to resolveParamTypeString(p.type, modelPackage, modulePackage),
                                )
                            },
                    ),
                )
            }
        }

    val useCaseHandlers =
        useCases.map { uc ->
            val intentName = uc.name.removeSuffix("UseCase")
            val rt = uc.returnType.orEmpty()
            val offsetPaged = PagePagingDetector.isOffsetPageResultUseCase(rt, pagingOpt, uc.parameters)
            val flowPaged = PagePagingDetector.isPaging3FlowUseCase(rt, pagingOpt)
            val pageParams = PagePagingDetector.detectPageParams(uc.parameters)
            val flowBasedRaw = PagePagingDetector.looksLikeFlowReturn(rt)
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
            val innerRt = PagePagingDetector.extractResultInnerType(rt)
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
            mapOf(
                "intentSimpleName" to intentName,
                "handlerName" to "handle${intentName.replaceFirstChar { it.uppercase() }}",
                "useCaseCamel" to uc.camelName,
                "hasParams" to uc.parameters.isNotEmpty(),
                "paramPassArgs" to uc.parameters.joinToString(", ") { "${it.name} = ${it.name}" },
                "showLoading" to !rt.contains("SseEmitter"),
                "resultBased" to resultBased,
                "flowBased" to flowBased,
                "pagedBased" to offsetPaged,
                "pagedFlowBased" to flowPaged,
                "pageParam" to pageParams.page,
                "pageSizeParam" to pageParams.pageSize,
                "pagedItemTypeFqn" to pagedItemFqnForUc,
                "pagedItemTypeContractRef" to
                    if (pagedItemFqnForUc.isNotBlank()) {
                        contractShortTypeDisplay(pagedItemFqnForUc)
                    } else {
                        ""
                    },
                "flowPagedItemTypeFqn" to flowItemFqnForUc,
                "flowPagedItemTypeContractRef" to
                    if (flowItemFqnForUc.isNotBlank()) {
                        contractShortTypeDisplay(flowItemFqnForUc)
                    } else {
                        ""
                    },
                "unitEntityEchoToState" to unitEcho,
                "unitEchoStatePropertyName" to echoProp,
                "unitEchoParamName" to echoParamName,
                "directReturnToState" to direct,
                "directStatePropertyName" to if (direct) uc.camelName else "",
            )
        }

    val hasResultBasedHandler =
        useCaseHandlers.any {
            it["resultBased"] == true || it["pagedBased"] == true
        } ||
            hasPagedOffset

    return mapOf(
        "pascalName" to pascalName,
        "camelName" to camelName,
        "modulePackage" to modulePackage,
        "coreBase" to coreBase,
        "screenPkg" to screenPkg,
        "modelPackage" to modelPackage,
        "resultClassFqn" to resultClassFqn,
        "useCases" to useCaseRows,
        "hasUseCases" to useCases.isNotEmpty(),
        "stateFields" to stateFields,
        "contractImports" to contractImports,
        "intentInners" to intentInners,
        "useCaseHandlers" to useCaseHandlers,
        "hasPagedOffset" to hasPagedOffset,
        "hasPagedFlow" to hasPagedFlow,
        "pagedItemTypeContractRef" to pagedItemContractRef,
        "pagedItemTypeFqn" to pagedItemFqn,
        "flowPagedItemTypeContractRef" to flowPagedItemContractRef,
        "defaultPageSize" to 20,
        "pagingOption" to pagingOpt,
        "hasResultBasedHandler" to hasResultBasedHandler,
        "primaryPagedArgList" to primaryPagedArgList,
        "primaryPagedUseCaseCamel" to (primaryOffsetUc?.camelName ?: ""),
        "primaryPagedNonPageArgList" to primaryPagedNonPageArgList,
        "pagingListStateInterfaceFqn" to "$coreBase.ui.PagingListState",
        "pageResultClassFqn" to "$coreBase.ui.PageResult",
        "pagedStateItemContractRef" to pagedItemContractRef,
        "pagedConcreteInnerContractRef" to pagedConcreteInnerShort,
    )
}

/** Inner type of `Flow<T>` / `StateFlow<T>` when [T] is not `PagingData<*>`. */
private fun extractFlowInnerType(returnType: String): String? {
    val m =
        Regex(
            """\b(?:kotlinx\.coroutines\.flow\.)?(?:Flow|StateFlow|SharedFlow|MutableStateFlow|MutableSharedFlow)\s*<\s*([^>]+)\s*>""",
        ).find(returnType) ?: return null
    val inner = m.groupValues[1].trim()
    if (inner.contains("PagingData")) return null
    val simple = inner.substringAfterLast(".")
    if (simple == "Unit" || inner == "kotlin.Unit") return null
    return inner
}

private fun buildKmpContractImports(
    stateFields: List<Map<String, Any?>>,
    hasPagedOffset: Boolean,
    coreBase: String,
): List<String> {
    val out = mutableSetOf<String>()
    stateFields.forEach { f ->
        val typeFqn = f["typeFqn"] as? String ?: return@forEach
        collectContractImportsForType(typeFqn, out)
    }
    if (hasPagedOffset) {
        out.add("import $coreBase.ui.DEFAULT_FIRST_PAGE")
        out.add("import $coreBase.ui.DEFAULT_PAGE_SIZE")
        out.add("import $coreBase.ui.PagingListState")
    }
    return out.sorted()
}
