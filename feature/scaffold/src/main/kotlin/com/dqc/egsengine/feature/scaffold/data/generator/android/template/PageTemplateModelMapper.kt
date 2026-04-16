/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

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
    val modelPackage = "$modulePackage.domain.model"
    val resultPackage = basePackage?.let { "$it.feature.base.domain.result" } ?: "com.example.feature.base.domain.result"
    val uiContractPackage = baseClassPackages.baseViewModel?.substringBeforeLast(".")
        ?: basePackage?.let { "$it.feature.base.presentation.viewmodel" }
        ?: "com.example.feature.base.presentation.viewmodel"

    val hasBaseViewModel = baseClassPackages.baseViewModel != null
    val baseVmFqcn = baseClassPackages.baseViewModel
    val baseVmSimple = baseVmFqcn?.substringAfterLast(".")
    val baseViewModelIsAndroidX = !hasBaseViewModel

    val useCaseModels = useCases.map { it.toPageUseCaseModel(modelPackage) }

    val stateFields = buildList {
        useCases.forEach { uc ->
            val rt = uc.returnType ?: return@forEach
            if (looksLikeFlowReturn(rt)) return@forEach
            val propName = uc.camelName
            val typeStr = resolveStatePropertyTypeString(rt, modelPackage)
            add(PageStateFieldModel(propName, typeStr, nullable = true))
        }
    }

    val intentInners = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        val emptyParams = uc.parameters.isEmpty()
        PageIntentInnerModel(
            simpleName = intentName,
            emptyParams = emptyParams,
            params = uc.parameters.map { it.toPageParamModel(modelPackage) },
        )
    }

    val useCaseHandlers = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        PageUseCaseHandlerModel(
            intentSimpleName = intentName,
            handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}",
            useCaseCamel = uc.camelName,
            hasParams = uc.parameters.isNotEmpty(),
            paramPassArgs = uc.parameters.joinToString(", ") { "${it.name} = ${it.name}" },
            showLoading = !(uc.returnType?.contains("SseEmitter") == true),
        )
    }

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
        intentInners = intentInners,
        useCaseHandlers = useCaseHandlers,
    )
}

private fun UseCaseParam.toPageParamModel(modelPackage: String): PageUseCaseParamModel =
    PageUseCaseParamModel(
        name = name,
        type = type,
        kotlinType = resolveParamTypeString(type, modelPackage),
        placeholderValue = placeholderValue,
    )

private fun UseCaseInfo.toPageUseCaseModel(modelPackage: String): PageUseCaseModel {
    val intentName = name.removeSuffix("UseCase")
    return PageUseCaseModel(
        name = name,
        camelName = camelName,
        packageName = packageName,
        returnType = returnType,
        parameters = parameters.map { it.toPageParamModel(modelPackage) },
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

private fun resolveStatePropertyTypeString(returnType: String, modelPackage: String): String {
    val innerType = extractResultInnerType(returnType)
        ?: Regex("""Result<([^>]+)>""").find(returnType)?.groupValues?.get(1)
        ?: returnType
    val simpleType = innerType.substringAfterLast(".")
    val typePackage = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
    return resolveBasicTypeString(simpleType, typePackage, modelPackage)
}

private fun resolveBasicTypeString(simpleType: String, typePackage: String, modelPackage: String): String {
    if (simpleType.startsWith("List<") && simpleType.endsWith(">")) {
        val innerType = simpleType.substring(5, simpleType.length - 1)
        val innerSimple = innerType.substringAfterLast(".")
        val innerPkg = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
        val inner = resolveBasicTypeString(innerSimple, innerPkg, modelPackage)
        return "kotlin.collections.List<$inner>"
    }
    return when (simpleType) {
        "Boolean", "ModelBoolean", "KotlinBoolean" -> "kotlin.Boolean"
        "Int", "ModelInt", "KotlinInt" -> "kotlin.Int"
        "Long", "ModelLong", "KotlinLong" -> "kotlin.Long"
        "String", "ModelString", "KotlinString" -> "kotlin.String"
        "Double", "ModelDouble", "KotlinDouble" -> "kotlin.Double"
        "Float", "ModelFloat", "KotlinFloat" -> "kotlin.Float"
        else ->
            if (simpleType.endsWith("ApiModel")) {
                "$modelPackage.${simpleType.removeSuffix("ApiModel")}"
            } else {
                "$typePackage.$simpleType"
            }
    }
}

private fun resolveParamTypeString(typeStr: String, modelPackage: String): String {
    val nullable = typeStr.endsWith("?")
    val base = typeStr.removeSuffix("?")
    val typeName = when {
        base.startsWith("List<") -> {
            val inner = Regex("""List<([^>]+)>""").find(base)?.groupValues?.get(1)
                ?: return "kotlin.collections.List"
            val innerType = resolveParamTypeString(inner, modelPackage)
            "kotlin.collections.List<$innerType>"
        }
        else -> base.split(".").let { parts ->
            val simple = parts.last()
            val pkg = if (parts.size > 1) parts.dropLast(1).joinToString(".") else modelPackage
            when (simple) {
                "Boolean", "ModelBoolean", "KotlinBoolean" -> "kotlin.Boolean"
                "Int", "ModelInt", "KotlinInt" -> "kotlin.Int"
                "Long", "ModelLong", "KotlinLong" -> "kotlin.Long"
                "String", "ModelString", "KotlinString" -> "kotlin.String"
                "Double", "ModelDouble", "KotlinDouble" -> "kotlin.Double"
                "Float", "ModelFloat", "KotlinFloat" -> "kotlin.Float"
                else ->
                    if (simple.endsWith("ApiModel")) {
                        "$modelPackage.${simple.removeSuffix("ApiModel")}"
                    } else {
                        "$pkg.$simple"
                    }
            }
        }
    }
    return if (nullable) "$typeName?" else typeName
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

internal fun buildParamTypeForTemplate(param: com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam, modelPackage: String): String =
    resolveParamTypeString(param.type, modelPackage)
