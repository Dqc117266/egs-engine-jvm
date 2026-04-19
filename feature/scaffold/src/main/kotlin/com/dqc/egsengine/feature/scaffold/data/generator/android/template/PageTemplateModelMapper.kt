/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.data.generator.common.PagePagingDetector
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import com.dqc.egsengine.template.model.BaseClassPackagesModel
import com.dqc.egsengine.template.model.PageIntentInnerModel
import com.dqc.egsengine.template.model.PageStateFieldModel
import com.dqc.egsengine.template.model.PageTemplateModel
import com.dqc.egsengine.template.model.PageUseCaseHandlerModel
import com.dqc.egsengine.template.model.PageUseCaseModel
import com.dqc.egsengine.template.model.PageUseCaseParamModel

internal fun PageTemplate.toPageTemplateModel(): PageTemplateModel {
    val pascalName = pageName
    val camelName = pascalName.replaceFirstChar { it.lowercase() }
    val layoutSnakeName = pascalName.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    val screenPkg = "$modulePackage.presentation.screen.$camelName"
    val screenDirPkg = screenPkg
    /** Swagger / API-sync generated DTOs (same as [KmpPageTemplateModelMapper]). */
    val modelPackage = "$modulePackage.generate.domain.model"
    val resultPackage = basePackage?.let { "$it.feature.base.domain.result" } ?: "com.example.feature.base.domain.result"
    val uiContractPackage = baseClassPackages.baseViewModel?.substringBeforeLast(".")
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
        pagedItemFqn = resolvePagedItemFqn(inner, modelPackage, modulePackage)
        pagedItemContractRef = contractShortTypeDisplay(pagedItemFqn)
    }
    var flowPagedItemContractRef = ""
    if (primaryFlowUc != null) {
        val raw = PagePagingDetector.extractPagingDataItemType(primaryFlowUc.returnType.orEmpty()) ?: ""
        val fqn = resolveParamTypeString(raw, modelPackage, modulePackage)
        flowPagedItemContractRef = contractShortTypeDisplay(fqn)
    }

    val useCaseModels = useCases.map { it.toPageUseCaseModel(modelPackage, modulePackage) }

    val stateFields = buildList {
        useCases.forEach { uc ->
            val rt = uc.returnType ?: return@forEach
            if (PagePagingDetector.isOffsetPageResultUseCase(rt, pagingOpt, uc.parameters)) return@forEach
            if (PagePagingDetector.isPaging3FlowUseCase(rt, pagingOpt)) return@forEach
            if (!shouldEmitStateFieldForReturnType(rt)) return@forEach
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

    val intentInners = buildList {
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

    val useCaseHandlers = useCases.map { uc ->
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
        val itemRaw = innerRt?.let {
            when {
                PagePagingDetector.isGenericPageResultType(it) -> PagePagingDetector.extractPageResultItemRaw(it)
                PagePagingDetector.isConcretePageResultInner(it) -> PagePagingDetector.extractConcretePageResultItemSimpleName(it)
                else -> null
            }
        } ?: ""
        val pagedItemFqnForUc = if (itemRaw.isNotBlank()) {
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
            pagedItemTypeContractRef = if (pagedItemFqnForUc.isNotBlank()) {
                contractShortTypeDisplay(pagedItemFqnForUc)
            } else {
                ""
            },
            flowPagedItemTypeFqn = flowItemFqnForUc,
            flowPagedItemTypeContractRef = if (flowItemFqnForUc.isNotBlank()) {
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
        baseClasses = BaseClassPackagesModel(
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

private fun buildPagedUseCaseArgumentList(
    uc: UseCaseInfo,
    pageParam: String,
    pageSizeParam: String,
): String =
    uc.parameters.joinToString(",\n            ") { p ->
        val value = when (p.name) {
            pageParam -> "page"
            pageSizeParam -> "size"
            else -> defaultLiteralForUseCaseParamType(p.type)
        }
        "${p.name} = $value"
    }

private fun defaultLiteralForUseCaseParamType(type: String): String {
    val t = type.trim()
    if (t.endsWith("?")) return "null"
    val base = t.removeSuffix("?").substringAfterLast(".")
    return when (base) {
        "Long" -> "0L"
        "Int" -> "0"
        "String" -> "\"\""
        "Boolean" -> "false"
        "Float" -> "0f"
        "Double" -> "0.0"
        else ->
            if (t.startsWith("List<") || t.contains(".List<")) {
                "emptyList()"
            } else {
                "null"
            }
    }
}

private fun resolvePagedItemFqn(innerResultType: String, modelPackage: String, modulePackage: String): String {
    val rt = innerResultType.trim()
    return when {
        PagePagingDetector.isGenericPageResultType(rt) -> {
            val raw = PagePagingDetector.extractPageResultItemRaw(rt) ?: return ""
            resolveParamTypeString(raw, modelPackage, modulePackage)
        }
        PagePagingDetector.isConcretePageResultInner(rt) -> {
            val simple = PagePagingDetector.extractConcretePageResultItemSimpleName(rt) ?: return ""
            resolveParamTypeString(simple, modelPackage, modulePackage)
        }
        else -> ""
    }
}

private fun UseCaseParam.toPageParamModel(modelPackage: String, modulePackage: String): PageUseCaseParamModel {
    val fqn = resolveParamTypeString(type, modelPackage, modulePackage)
    return PageUseCaseParamModel(
        name = name,
        type = type,
        kotlinType = fqn,
        kotlinTypeContractRef = contractShortTypeDisplay(fqn),
        placeholderValue = placeholderValue,
    )
}

private fun UseCaseInfo.toPageUseCaseModel(modelPackage: String, modulePackage: String): PageUseCaseModel {
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

/**
 * Unit-returning DB / prefs use cases that take a single argument — Insert / InsertAll / Update /
 * UpdateAll / Delete / DeleteAll / Set. We echo the caller-provided argument into State so Compose
 * can observe the last successful write without rewriting the UseCase signature.
 *
 * DeleteAll-style no-arg variants fall through to the default `// TODO` path since there is
 * nothing meaningful to echo.
 */
internal fun isUnitParamEchoUseCase(
    uc: UseCaseInfo,
    @Suppress("UNUSED_PARAMETER") modelPackage: String,
    @Suppress("UNUSED_PARAMETER") modulePackage: String,
): Boolean {
    if (!uc.name.endsWith("UseCase")) return false
    val rt = uc.returnType?.trim()
    if (!rt.isNullOrBlank() && rt != "Unit" && rt != "kotlin.Unit") return false
    if (uc.parameters.size != 1) return false
    val base = uc.name.removeSuffix("UseCase")
    return UNIT_ECHO_PREFIX_REGEX.containsMatchIn(base)
}

private val UNIT_ECHO_PREFIX_REGEX =
    Regex("""^(UpdateAll|InsertAll|DeleteAll|Update|Insert|Delete|Set)(?=[A-Z]|$)""")

/**
 * State property name for [isUnitParamEchoUseCase] hits.
 *
 * For backward compatibility with existing Contracts, `Update*(entity: *Entity)` keeps the legacy
 * `updatedFoo` naming. Every other matched prefix (Insert / InsertAll / Delete / DeleteAll / Set /
 * UpdateAll) uses the use case's camelName directly (matches `handleGetUserId`-style).
 */
internal fun unitEchoStatePropertyNameFor(uc: UseCaseInfo, paramFqn: String): String {
    val base = uc.name.removeSuffix("UseCase")
    if (base.startsWith("Update") && !base.startsWith("UpdateAll")) {
        val simple = paramFqn.trimEnd('?').substringAfterLast(".").removeSuffix("Entity")
        if (simple.isNotEmpty() && paramFqn.trimEnd('?').substringAfterLast(".").endsWith("Entity")) {
            return "updated" + simple.replaceFirstChar { it.uppercase() }
        }
    }
    return uc.camelName
}

/**
 * Prefs `Get*UseCase` / plain suspend APIs that return a value directly (not [Result], not Flow).
 * Maps invoke result into State under [UseCaseInfo.camelName].
 */
private fun isDirectReturnToStateUseCase(
    uc: UseCaseInfo,
    modelPackage: String,
    modulePackage: String,
): Boolean {
    val rt = uc.returnType?.trim() ?: return false
    if (rt.isBlank()) return false
    if (looksLikeFlowReturn(rt)) return false
    if (looksLikeResultReturn(rt)) return false
    if (isUnitParamEchoUseCase(uc, modelPackage, modulePackage)) return false
    if (!shouldEmitStateFieldForReturnType(rt)) return false
    return true
}

/** No [State] field for Flow returns or for Unit / Result<Unit> (side-effect DB writes). */
private fun shouldEmitStateFieldForReturnType(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    if (looksLikeFlowReturn(returnType)) return false
    val norm = shortenKotlinStdlibPrimitiveFqns(returnType.trim())
    val effective = extractResultInnerType(norm)
        ?: Regex("""Result<([^>]+)>""").find(norm)?.groupValues?.get(1)?.trim()
        ?: norm
    val trimmed = effective.trimEnd('?')
    val simple = trimmed.substringAfterLast(".")
    if (simple == "Unit" || trimmed == "kotlin.Unit") return false
    return true
}

/**
 * Use case sources may use fully qualified stdlib types (`kotlin.Long`, `kotlin.Int`).
 * Generated Contract / ViewModel should use short names (`Long`, `Int`) without extra imports.
 */
private fun shortenKotlinStdlibPrimitiveFqns(typeStr: String): String {
    var s = typeStr
    val nonChar = Regex("""kotlin\.(Long|Int|String|Boolean|Double|Float|Byte|Short)(\?)?""")
    s = nonChar.replace(s) { m -> m.groupValues[1] + m.groupValues[2] }
    val charOnly = Regex("""kotlin\.Char(\?)?(?![a-zA-Z])""")
    s = charOnly.replace(s) { m -> "Char" + m.groupValues[1] }
    return s
}

private fun resolveStatePropertyTypeString(returnType: String, modelPackage: String, modulePackage: String): String {
    val returnTypeNorm = shortenKotlinStdlibPrimitiveFqns(returnType.trim())
    val innerType = extractResultInnerType(returnTypeNorm)
        ?: Regex("""Result<([^>]+)>""").find(returnTypeNorm)?.groupValues?.get(1)?.trim()
        ?: returnTypeNorm
    val simpleType = innerType.substringAfterLast(".")
    val typePackage = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
    return resolveBasicTypeString(simpleType, typePackage, modelPackage, modulePackage)
}

private fun extractResultInnerType(returnType: String): String? {
    val idx = returnType.indexOf("Result<")
    if (idx < 0) return null
    val start = idx + "Result<".length
    var depth = 1
    var i = start
    while (i < returnType.length && depth > 0) {
        when (returnType[i]) {
            '<' -> depth++
            '>' -> depth--
        }
        i++
    }
    if (depth != 0) return null
    return returnType.substring(start, i - 1).trim()
}

private fun resolveBasicTypeString(simpleType: String, typePackage: String, modelPackage: String, modulePackage: String): String {
    val cleaned = shortenKotlinStdlibPrimitiveFqns(simpleType)
    if (cleaned.startsWith("List<") && cleaned.endsWith(">")) {
        val innerType = extractFirstGenericArgument(cleaned, "List<")
            ?: cleaned.substring(5, cleaned.length - 1)
        val innerSimple = innerType.substringAfterLast(".")
        val innerPkg = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
        val inner = resolveBasicTypeString(innerSimple, innerPkg, modelPackage, modulePackage)
        return "List<$inner>"
    }
    return when (cleaned) {
        "Boolean", "ModelBoolean", "KotlinBoolean" -> "Boolean"
        "Int", "ModelInt", "KotlinInt" -> "Int"
        "Long", "ModelLong", "KotlinLong" -> "Long"
        "String", "ModelString", "KotlinString" -> "String"
        "Double", "ModelDouble", "KotlinDouble" -> "Double"
        "Float", "ModelFloat", "KotlinFloat" -> "Float"
        else -> {
            val raw = if (cleaned.endsWith("ApiModel")) {
                "$modelPackage.${cleaned.removeSuffix("ApiModel")}"
            } else {
                "$typePackage.$cleaned"
            }
            fixRoomEntityFqn(normalizeSwaggerModelFqn(raw, modulePackage), cleaned, modulePackage)
        }
    }
}

private fun resolveParamTypeString(typeStr: String, modelPackage: String, modulePackage: String): String {
    val normalized = shortenKotlinStdlibPrimitiveFqns(typeStr.trim())
    val nullable = normalized.endsWith("?")
    val base = normalized.removeSuffix("?")
    val typeName = when {
        base.startsWith("List<") -> {
            val inner = extractFirstGenericArgument(base, "List<")
                ?: Regex("""List<([^>]+)>""").find(base)?.groupValues?.get(1)
                ?: return "List"
            val innerType = resolveParamTypeString(inner, modelPackage, modulePackage)
            "List<$innerType>"
        }
        else -> base.split(".").let { parts ->
            val simple = parts.last()
            val pkg = if (parts.size > 1) parts.dropLast(1).joinToString(".") else modelPackage
            when (simple) {
                "Boolean", "ModelBoolean", "KotlinBoolean" -> "Boolean"
                "Int", "ModelInt", "KotlinInt" -> "Int"
                "Long", "ModelLong", "KotlinLong" -> "Long"
                "String", "ModelString", "KotlinString" -> "String"
                "Double", "ModelDouble", "KotlinDouble" -> "Double"
                "Float", "ModelFloat", "KotlinFloat" -> "Float"
                else ->
                    if (simple.endsWith("ApiModel")) {
                        "$modelPackage.${simple.removeSuffix("ApiModel")}"
                    } else if (simple.endsWith("Entity")) {
                        val entityPkg = databaseEntityPackage(modulePackage)
                        val candidate = when {
                            parts.size == 1 -> "$entityPkg.$simple"
                            pkg == modelPackage || pkg.endsWith(".generate.domain.model") -> "$entityPkg.$simple"
                            else -> "$pkg.$simple"
                        }
                        fixRoomEntityFqn(normalizeSwaggerModelFqn(candidate, modulePackage), simple, modulePackage)
                    } else {
                        normalizeSwaggerModelFqn("$pkg.$simple", modulePackage)
                    }
            }
        }
    }
    return if (nullable) "$typeName?" else typeName
}

private fun databaseEntityPackage(modulePackage: String): String =
    "$modulePackage.generate.data.datasource.database.entity"

/** Room entities belong under [databaseEntityPackage], not [generate.domain.model]. */
private fun fixRoomEntityFqn(fqn: String, simple: String, modulePackage: String): String {
    if (!simple.endsWith("Entity")) return fqn
    val entityPkg = databaseEntityPackage(modulePackage)
    val wrong = "$modulePackage.generate.domain.model.$simple"
    if (fqn == wrong) return "$entityPkg.$simple"
    return fqn
}

/** API-sync DTOs live under [modulePackage].generate.domain.model; older sources may still say …domain.model…. */
private fun normalizeSwaggerModelFqn(fqn: String, modulePackage: String): String {
    val legacy = "$modulePackage.domain.model."
    if (fqn.startsWith(legacy)) {
        return fqn.replaceFirst(legacy, "$modulePackage.generate.domain.model.")
    }
    return fqn
}

private fun contractShortTypeDisplay(typeFqn: String): String {
    val trimmed = typeFqn.trimEnd('?')
    val nullable = typeFqn.endsWith("?")
    val base = contractShortTypeDisplayInner(trimmed)
    return if (nullable) "$base?" else base
}

internal fun contractShortTypeDisplayInner(s: String): String {
    if (s.startsWith("List<") && s.endsWith(">")) {
        val inner = extractFirstGenericArgument(s, "List<") ?: return s
        return "List<${contractShortTypeDisplayInner(inner)}>"
    }
    val open = s.indexOf('<')
    if (open > 0 && s.endsWith(">")) {
        val args = s.substring(open + 1, s.length - 1)
        val outerShort = s.substring(0, open).substringAfterLast(".")
        val innerShort = splitTopLevelCommaGenericArgs(args).joinToString(", ") { contractShortTypeDisplayInner(it.trim()) }
        return "$outerShort<$innerShort>"
    }
    return if (s.contains(".")) s.substringAfterLast(".") else s
}

private fun splitTopLevelCommaGenericArgs(args: String): List<String> {
    val out = mutableListOf<String>()
    var depth = 0
    var start = 0
    var i = 0
    while (i <= args.length) {
        val c = args.getOrNull(i)
        if (c == null || (c == ',' && depth == 0)) {
            val part = args.substring(start, i).trim()
            if (part.isNotEmpty()) out.add(part)
            start = i + 1
        } else when (c) {
            '<' -> depth++
            '>' -> depth--
        }
        i++
    }
    return out
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

/** Import lines (`import …`) for types appearing in ViewModel handler parameters (e.g. `List<UserEntity>` → `UserEntity`). */
internal fun importLinesForKotlinTypeFqns(fqns: Iterable<String>): List<String> {
    val out = mutableSetOf<String>()
    fqns.forEach { collectContractImportsForType(it, out) }
    return out.sorted()
}

internal fun importLinesForUseCaseHandlerParams(parameters: List<PageUseCaseParamModel>): List<String> =
    importLinesForKotlinTypeFqns(parameters.map { it.kotlinType })

internal fun collectContractImportsForType(typeFqn: String, out: MutableSet<String>) {
    val trimmed = typeFqn.trimEnd('?')
    if (trimmed.startsWith("List<") && trimmed.endsWith(">")) {
        val inner = extractFirstGenericArgument(trimmed, "List<") ?: return
        collectContractImportsForType(inner, out)
        return
    }
    if (shouldEmitImportForFqn(trimmed)) {
        out.add("import $trimmed")
    }
}

private fun extractFirstGenericArgument(s: String, prefix: String): String? {
    if (!s.startsWith(prefix) || !s.endsWith(">")) return null
    var start = prefix.length
    var depth = 1
    var i = start
    while (i < s.length && depth > 0) {
        when (s[i]) {
            '<' -> depth++
            '>' -> depth--
        }
        i++
    }
    if (depth != 0) return null
    return s.substring(start, i - 1).trim()
}

private fun shouldEmitImportForFqn(typeFqn: String): Boolean {
    if (!typeFqn.contains(".")) return false
    if (typeFqn.startsWith("kotlin.")) return false
    if (typeFqn.startsWith("java.")) return false
    return true
}

internal fun buildParamTypeForTemplate(
    param: com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam,
    modelPackage: String,
    modulePackage: String,
): String = resolveParamTypeString(param.type, modelPackage, modulePackage)
