/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.android.template.importLinesForKotlinTypeFqns
import com.dqc.egsengine.feature.scaffold.data.generator.common.ViewModelMergeSnippet
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo

/**
 * Incremental snippets for [ViewModelMemberMerger], aligned with
 * [PageTemplate.toKmpPageTemplateMap], [PageContract.kt.ftl], [PageViewModel.kt.ftl].
 */
internal fun buildMergeSnippetForUseCase(
    template: PageTemplate,
    templateMap: Map<String, Any?>,
    ucIndex: Int,
): ViewModelMergeSnippet {
    val pascalName = templateMap["pascalName"] as String
    val useCases = templateMap["useCases"] as List<Map<String, Any?>>
    val ucRow = useCases[ucIndex]
    val uc = template.useCases[ucIndex]
    val handlers = templateMap["useCaseHandlers"] as List<Map<String, Any?>>
    val h = handlers[ucIndex]
    val intentInners = templateMap["intentInners"] as List<Map<String, Any?>>
    val stateFields = templateMap["stateFields"] as List<Map<String, Any?>>

    val intentSimpleName = uc.name.removeSuffix("UseCase")
    val intentInner = intentInners.find { (it["simpleName"] as? String) == intentSimpleName }

    val vmImport = "import ${uc.packageName}.${uc.name}"
    val ctorParamLine = "    private val ${uc.camelName}: ${uc.name},\n"
    val paramTypeFqns =
        (ucRow["parameters"] as? List<Map<String, Any?>>).orEmpty().mapNotNull { it["kotlinType"] as? String }
    fun vmImportLinesForMerge(): List<String> =
        (listOf(vmImport) + importLinesForKotlinTypeFqns(paramTypeFqns)).distinct().sorted()

    val pagedBased = h["pagedBased"] == true
    if (pagedBased) {
        return ViewModelMergeSnippet(
            useCase = uc,
            intentMemberText = "",
            stateFieldText = null,
            viewModelImportLines = vmImportLinesForMerge(),
            ctorParamLine = ctorParamLine,
            registerIntentBlock = "",
            handlerFunction = null,
        )
    }

    val intentInnerNonNull =
        intentInner ?: error("intent inner not found for $intentSimpleName (expected for non-paged use case)")

    val intentMemberText = renderIntentMember(intentInnerNonNull)
    val stateFieldText = resolveStateFieldSnippet(stateFields, uc, h)
    val registerBlock =
        renderRegisterIntentBlock(
            pascalName = pascalName,
            ucRow = ucRow,
            intentInner = intentInnerNonNull,
            templateMap = templateMap,
        )
    val handlerFunction = renderHandlerFunction(pascalName, uc, ucRow, h)

    return ViewModelMergeSnippet(
        useCase = uc,
        intentMemberText = intentMemberText,
        stateFieldText = stateFieldText,
        viewModelImportLines = vmImportLinesForMerge(),
        ctorParamLine = ctorParamLine,
        registerIntentBlock = registerBlock,
        handlerFunction = handlerFunction,
    )
}

private fun renderIntentMember(intentInner: Map<String, Any?>): String {
    val simpleName = intentInner["simpleName"] as String
    val emptyParams = intentInner["emptyParams"] as Boolean
    return if (emptyParams) {
        "\n        data object $simpleName : Intent()"
    } else {
        val params = intentInner["params"] as List<Map<String, String>>
        val lines = params.joinToString(",\n") { p ->
            "            val ${p["name"]}: ${p["kotlinType"]}"
        }
        "\n        data class $simpleName(\n$lines\n        ) : Intent()"
    }
}

private fun resolveStateFieldSnippet(
    stateFields: List<Map<String, Any?>>,
    uc: UseCaseInfo,
    h: Map<String, Any?>,
): String? {
    val name: String =
        when {
            h["unitEntityEchoToState"] == true ->
                h["unitEchoStatePropertyName"] as? String ?: return null
            h["directReturnToState"] == true ->
                h["directStatePropertyName"] as? String ?: return null
            else -> uc.camelName
        }
    val field = stateFields.find { (it["name"] as? String) == name } ?: return null
    val nullable = field["nullable"] as Boolean
    val typeRef = field["typeContractRef"] as String
    val indent = "\n        "
    return if (nullable) {
        "${indent}val $name: $typeRef? = null,"
    } else {
        val default = defaultValueForStateField(field["name"] as String)
        "${indent}val $name: $typeRef = $default,"
    }
}

private fun defaultValueForStateField(name: String): String =
    when (name) {
        "items" -> "emptyList()"
        "page" -> "0"
        "pageSize" -> "20"
        "total" -> "0L"
        "totalPages" -> "0"
        "endReached", "isRefreshing", "isLoadingMore" -> "false"
        else -> "emptyList()"
    }

@Suppress("UNCHECKED_CAST")
private fun renderRegisterIntentBlock(
    pascalName: String,
    ucRow: Map<String, Any?>,
    intentInner: Map<String, Any?>,
    templateMap: Map<String, Any?>,
): String {
    val hasPagedOffset = templateMap["hasPagedOffset"] == true
    val intentSimpleName = intentInner["simpleName"] as String
    val emptyParams = intentInner["emptyParams"] as Boolean
    val handlerName = ucRow["handlerName"] as String
    val intentParams = intentInner["params"] as? List<Map<String, String>> ?: emptyList()

    val body =
        when {
            hasPagedOffset && (intentSimpleName == "Refresh" || intentSimpleName == "Retry") ->
                "runPagedLoad(refresh = true)"
            hasPagedOffset && intentSimpleName == "LoadMore" ->
                "runPagedLoad(refresh = false)"
            emptyParams ->
                "$handlerName()"
            else -> {
                val args = intentParams.joinToString(", ") { "it.${it["name"]}" }
                "$handlerName($args)"
            }
        }

    return """
        registerIntent<${pascalName}Contract.Intent.$intentSimpleName> {
            $body
        }
    """.trimIndent()
}

@Suppress("UNCHECKED_CAST")
private fun renderHandlerFunction(
    pascalName: String,
    uc: UseCaseInfo,
    ucRow: Map<String, Any?>,
    h: Map<String, Any?>,
): String {
    val params = ucRow["parameters"] as List<Map<String, String>>
    val paramList =
        params.joinToString(", ") { p ->
            "${p["name"]}: ${p["kotlinType"]}"
        }
    val paramPass = params.joinToString(", ") { "${it["name"]} = ${it["name"]}" }
    val handlerName = h["handlerName"] as String
    val useCaseCamel = h["useCaseCamel"] as String
    val showLoading = h["showLoading"] as? Boolean ?: true

    return when {
        h["pagedFlowBased"] == true ->
            """
    private fun $handlerName($paramList) {
        launch {
            $useCaseCamel($paramPass).collect { pagingData ->
                // Use androidx.paging.compose.collectAsLazyPagingItems(pagingData) on Android, or map PagingData in platform code.
                updateState { copy(error = null) }
            }
        }
    }
""".trimIndent() + "\n"

        h["resultBased"] == true ->
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launchRequest(showLoading = $showLoading) {
            when (val result = $useCaseCamel($paramPass)) {
                is Result.Success -> {
                    updateState { copy($useCaseCamel = result.value) }
                }
                is Result.Failure -> {
                    updateState { copy(error = result.throwable?.message) }
                }
            }
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launchRequest(showLoading = $showLoading) {
            when (val result = $useCaseCamel()) {
                is Result.Success -> {
                    updateState { copy($useCaseCamel = result.value) }
                }
                is Result.Failure -> {
                    updateState { copy(error = result.throwable?.message) }
                }
            }
        }
    }
""".trimIndent() + "\n"
            }

        h["flowBased"] == true ->
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launch {
            $useCaseCamel($paramPass).collect { value ->
                updateState { copy($useCaseCamel = value, error = null) }
            }
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launch {
            $useCaseCamel().collect { value ->
                updateState { copy($useCaseCamel = value, error = null) }
            }
        }
    }
""".trimIndent() + "\n"
            }

        h["unitEntityEchoToState"] == true -> {
            val echoProp = h["unitEchoStatePropertyName"] as String
            val echoParam = h["unitEchoParamName"] as String
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launchRequest {
            $useCaseCamel($paramPass)
            updateState { copy($echoProp = $echoParam, error = null) }
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launchRequest {
            $useCaseCamel()
        }
    }
""".trimIndent() + "\n"
            }
        }

        h["directReturnToState"] == true -> {
            val prop = h["directStatePropertyName"] as String
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launchRequest {
            val ret = $useCaseCamel($paramPass)
            updateState { copy($prop = ret, error = null) }
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launchRequest {
            val ret = $useCaseCamel()
            updateState { copy($prop = ret, error = null) }
        }
    }
""".trimIndent() + "\n"
            }
        }

        else ->
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launchRequest {
            $useCaseCamel($paramPass)
            // TODO: map result to State (or add Result / Flow return type to UseCase)
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launchRequest {
            $useCaseCamel()
            // TODO: map result to State (or add Result / Flow return type to UseCase)
        }
    }
""".trimIndent() + "\n"
            }
    }
}

