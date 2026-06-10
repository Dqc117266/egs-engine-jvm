/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.data.generator.common.ViewModelMergeSnippet
import com.dqc.egsengine.feature.scaffold.data.generator.common.importLinesForUseCaseHandlerParams
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.template.model.PageIntentInnerModel
import com.dqc.egsengine.template.model.PageStateFieldModel
import com.dqc.egsengine.template.model.PageTemplateModel
import com.dqc.egsengine.template.model.PageUseCaseHandlerModel
import com.dqc.egsengine.template.model.PageUseCaseModel

/**
 * Incremental snippets aligned with [android/page/PageContract.kt.ftl] and [android/page/PageViewModel.kt.ftl].
 */
internal fun buildAndroidMergeSnippetForUseCase(
    template: PageTemplate,
    model: PageTemplateModel,
    ucIndex: Int,
): ViewModelMergeSnippet {
    val uc = template.useCases[ucIndex]
    val ucRow = model.useCases[ucIndex]
    val h = model.useCaseHandlers[ucIndex]
    val intentInner = model.intentInners[ucIndex]

    val vmImports = mutableListOf<String>()
    if (h.resultBased || h.pagedBased) {
        vmImports.add("import ${model.resultPackage}.Result")
    }
    if (h.pagedBased) {
        vmImports.add("import ${model.pageResultClassFqn}")
    }
    vmImports.add("import ${uc.packageName}.${uc.name}")
    vmImports.addAll(importLinesForUseCaseHandlerParams(ucRow.parameters))

    val ctorParamLine = "    private val ${uc.camelName}: ${uc.name},\n"

    if (h.pagedBased) {
        return ViewModelMergeSnippet(
            useCase = uc,
            intentMemberText = "",
            stateFieldText = null,
            viewModelImportLines = vmImports.distinct().sorted(),
            ctorParamLine = ctorParamLine,
            registerIntentBlock = "",
            handlerFunction = null,
        )
    }

    val intentMemberText = renderAndroidIntentMember(intentInner)
    val stateFieldText = resolveAndroidStateFieldSnippet(model, uc, h)
    val registerBlock = renderAndroidRegisterBlock(model.pascalName, ucRow)
    val handlerFunction = renderAndroidHandlerFunction(ucRow, h)

    return ViewModelMergeSnippet(
        useCase = uc,
        intentMemberText = intentMemberText,
        stateFieldText = stateFieldText,
        viewModelImportLines = vmImports.distinct().sorted(),
        ctorParamLine = ctorParamLine,
        registerIntentBlock = registerBlock,
        handlerFunction = handlerFunction,
    )
}

/**
 * When an existing Contract has no paging yet, inserts Refresh/LoadMore/Retry, paging [State] fields,
 * register blocks, and [loadPage] (idempotent via merger name checks).
 */
internal fun buildAndroidPagingBootstrapSnippets(
    template: PageTemplate,
    model: PageTemplateModel,
): List<ViewModelMergeSnippet> {
    if (!model.hasPagedOffset) return emptyList()
    val primaryIdx = model.useCaseHandlers.indexOfFirst { it.pagedBased }
    if (primaryIdx < 0) return emptyList()
    val uc = template.useCases[primaryIdx]
    val out = mutableListOf<ViewModelMergeSnippet>()
    val pagingNames =
        setOf("items", "total", "page", "pageSize", "isRefreshing", "isLoadingMore", "endReached", "pagingError")
    for (f in model.stateFields.filter { it.name in pagingNames }) {
        out +=
            ViewModelMergeSnippet(
                useCase = uc,
                intentMemberText = "",
                stateFieldText = renderAndroidPagingStateFieldLine(f),
                viewModelImportLines = emptyList(),
                ctorParamLine = "",
                registerIntentBlock = "",
                handlerFunction = null,
            )
    }
    for (name in listOf("Refresh", "LoadMore", "Retry")) {
        out +=
            ViewModelMergeSnippet(
                useCase = uc,
                intentMemberText = "\n        data object $name : Intent",
                stateFieldText = null,
                viewModelImportLines = emptyList(),
                ctorParamLine = "",
                registerIntentBlock = "",
                handlerFunction = null,
            )
    }
    for (intent in listOf("Refresh", "Retry", "LoadMore")) {
        val body =
            if (intent == "LoadMore") {
                "loadPage(refresh = false)"
            } else {
                "loadPage(refresh = true)"
            }
        out +=
            ViewModelMergeSnippet(
                useCase = uc,
                intentMemberText = "",
                stateFieldText = null,
                viewModelImportLines = emptyList(),
                ctorParamLine = "",
                registerIntentBlock =
                """
                    registerIntent<${model.pascalName}Contract.Intent.$intent> {
                        $body
                    }
                """.trimIndent(),
                handlerFunction = null,
            )
    }
    out +=
        ViewModelMergeSnippet(
            useCase = uc,
            intentMemberText = "",
            stateFieldText = null,
            viewModelImportLines =
            listOf(
                "import ${model.resultPackage}.Result",
                "import ${model.pageResultClassFqn}",
            ).sorted(),
            ctorParamLine = "",
            registerIntentBlock = "",
            handlerFunction = renderAndroidLoadPageHandler(model),
        )
    return out
}

private fun renderAndroidPagingStateFieldLine(f: PageStateFieldModel): String {
    val indent = "\n        "
    return if (f.nullable) {
        "${indent}override val ${f.name}: ${f.typeContractRef}? = null,"
    } else {
        "${indent}override val ${f.name}: ${f.typeContractRef} = ${f.defaultLiteral},"
    }
}

private fun renderAndroidLoadPageHandler(model: PageTemplateModel): String {
    val argList = model.primaryPagedArgList.ifBlank { "page = page, size = size" }
    return androidHandlerBlock(
        """
    private fun loadPage(refresh: Boolean) {
        runPagedLoad<${model.pagedStateItemContractRef}>(refresh = refresh) { page, size ->
            when (val result = ${model.primaryPagedUseCaseCamel}($argList)) {
                is Result.Success -> {
                    val data = result.value
                    PageResult(
                        list = data.list,
                        total = data.total,
                        page = page,
                        pageSize = size,
                    )
                }
                is Result.Failure -> throw (result.throwable ?: IllegalStateException("Paging error"))
            }
        }
    }
""",
    )
}

private fun renderAndroidIntentMember(intentInner: PageIntentInnerModel): String {
    val simpleName = intentInner.simpleName
    return if (intentInner.emptyParams) {
        "\n        data object $simpleName : Intent"
    } else {
        val lines =
            intentInner.params.joinToString(",\n") { p ->
                "            val ${p.name}: ${p.kotlinTypeContractRef}"
            }
        "\n        data class $simpleName(\n$lines\n        ) : Intent"
    }
}

private fun resolveAndroidStateFieldSnippet(
    model: PageTemplateModel,
    uc: UseCaseInfo,
    h: PageUseCaseHandlerModel,
): String? {
    val name: String =
        when {
            h.unitEntityEchoToState -> h.unitEchoStatePropertyName.ifEmpty { return null }
            h.directReturnToState -> h.directStatePropertyName.ifEmpty { return null }
            else -> uc.camelName
        }
    val field = model.stateFields.find { it.name == name } ?: return null
    val typeRef = field.typeContractRef
    val indent = "\n        "
    return "${indent}val ${field.name}: $typeRef? = null,"
}

private fun renderAndroidRegisterBlock(
    pascalName: String,
    uc: PageUseCaseModel,
): String {
    val body =
        if (uc.parameters.isNotEmpty()) {
            val args = uc.parameters.joinToString(", ") { "it.${it.name}" }
            "${uc.handlerName}($args)"
        } else {
            "${uc.handlerName}()"
        }
    return """
        registerIntent<${pascalName}Contract.Intent.${uc.intentName}> {
            $body
        }
    """.trimIndent()
}

/**
 * Raw handler snippets use [trimIndent], which strips the shared margin and often leaves `private fun`
 * at column 0. [prependIndent] restores class-body indentation (4 spaces) while preserving nesting.
 */
private fun androidHandlerBlock(raw: String): String = raw.trimIndent().prependIndent("    ").trimEnd() + "\n"

private fun renderAndroidHandlerFunction(
    ucRow: PageUseCaseModel,
    h: PageUseCaseHandlerModel,
): String {
    val params = ucRow.parameters
    val paramList = params.joinToString(", ") { "${it.name}: ${it.kotlinTypeContractRef}" }
    val paramPass = params.joinToString(", ") { "${it.name} = ${it.name}" }
    val handlerName = h.handlerName
    val useCaseCamel = h.useCaseCamel
    val showLoading = h.showLoading

    return when {
        h.pagedFlowBased ->
            if (params.isNotEmpty()) {
                androidHandlerBlock(
                    """
    private fun $handlerName($paramList) {
        launch {
            $useCaseCamel($paramPass).collect { pagingData ->
                // Use androidx.paging.compose.collectAsLazyPagingItems(pagingData) in UI.
                updateState { copy(error = null) }
            }
        }
    }
""",
                )
            } else {
                androidHandlerBlock(
                    """
    private fun $handlerName() {
        launch {
            $useCaseCamel().collect { pagingData ->
                updateState { copy(error = null) }
            }
        }
    }
""",
                )
            }

        h.resultBased ->
            if (params.isNotEmpty()) {
                androidHandlerBlock(
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
""",
                )
            } else {
                androidHandlerBlock(
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
""",
                )
            }

        h.flowBased ->
            if (params.isNotEmpty()) {
                androidHandlerBlock(
                    """
    private fun $handlerName($paramList) {
        launch {
            $useCaseCamel($paramPass)
            // TODO: collect Flow and update State
        }
    }
""",
                )
            } else {
                androidHandlerBlock(
                    """
    private fun $handlerName() {
        launch {
            $useCaseCamel()
            // TODO: collect Flow and update State
        }
    }
""",
                )
            }

        h.unitEntityEchoToState ->
            if (params.isNotEmpty()) {
                androidHandlerBlock(
                    """
    private fun $handlerName($paramList) {
        launchRequest {
            $useCaseCamel($paramPass)
            updateState { copy(${h.unitEchoStatePropertyName} = ${h.unitEchoParamName}, error = null) }
        }
    }
""",
                )
            } else {
                androidHandlerBlock(
                    """
    private fun $handlerName() {
        launchRequest {
            $useCaseCamel()
        }
    }
""",
                )
            }

        h.directReturnToState ->
            if (params.isNotEmpty()) {
                androidHandlerBlock(
                    """
    private fun $handlerName($paramList) {
        launchRequest {
            val ret = $useCaseCamel($paramPass)
            updateState { copy(${h.directStatePropertyName} = ret, error = null) }
        }
    }
""",
                )
            } else {
                androidHandlerBlock(
                    """
    private fun $handlerName() {
        launchRequest {
            val ret = $useCaseCamel()
            updateState { copy(${h.directStatePropertyName} = ret, error = null) }
        }
    }
""",
                )
            }

        else ->
            if (params.isNotEmpty()) {
                androidHandlerBlock(
                    """
    private fun $handlerName($paramList) {
        launchRequest {
            $useCaseCamel($paramPass)
            // TODO: map result to State (or add Result / Flow return type to UseCase)
        }
    }
""",
                )
            } else {
                androidHandlerBlock(
                    """
    private fun $handlerName() {
        launchRequest {
            $useCaseCamel()
            // TODO: map result to State (or add Result / Flow return type to UseCase)
        }
    }
""",
                )
            }
    }
}
