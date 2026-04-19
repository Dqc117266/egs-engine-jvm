/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.data.generator.common.ViewModelMergeSnippet
import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.template.model.PageIntentInnerModel
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
    if (h.resultBased) {
        vmImports.add("import ${model.resultPackage}.Result")
    }
    vmImports.add("import ${uc.packageName}.${uc.name}")

    val ctorParamLine = "    private val ${uc.camelName}: ${uc.name},\n"

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

private fun renderAndroidRegisterBlock(pascalName: String, uc: PageUseCaseModel): String {
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
        h.resultBased ->
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

        h.flowBased ->
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launch {
            $useCaseCamel($paramPass)
            // TODO: collect Flow and update State
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launch {
            $useCaseCamel()
            // TODO: collect Flow and update State
        }
    }
""".trimIndent() + "\n"
            }

        h.unitEntityEchoToState ->
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launchRequest {
            $useCaseCamel($paramPass)
            updateState { copy(${h.unitEchoStatePropertyName} = ${h.unitEchoParamName}, error = null) }
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

        h.directReturnToState ->
            if (params.isNotEmpty()) {
                """
    private fun $handlerName($paramList) {
        launchRequest {
            val ret = $useCaseCamel($paramPass)
            updateState { copy(${h.directStatePropertyName} = ret, error = null) }
        }
    }
""".trimIndent() + "\n"
            } else {
                """
    private fun $handlerName() {
        launchRequest {
            val ret = $useCaseCamel()
            updateState { copy(${h.directStatePropertyName} = ret, error = null) }
        }
    }
""".trimIndent() + "\n"
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
