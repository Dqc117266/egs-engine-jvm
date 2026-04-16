/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo

/**
 * Builds a FreeMarker root model for KMP page templates under `templates/kmp/page/`.
 * Package layout: [modulePackage].presentation.[camelPageName] (no extra `screen` path segment).
 */
internal fun PageTemplate.toKmpPageTemplateMap(): Map<String, Any?> {
    val pascalName = pageName
    val camelName = pascalName.replaceFirstChar { it.lowercase() }
    val modelPackage = "$modulePackage.domain.model"
    val screenPkg = "$modulePackage.presentation.$camelName"
    val resultPackage = "template.core.base.network.domain"

    val useCaseRows = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        val handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}"
        mapOf(
            "name" to uc.name,
            "camelName" to uc.camelName,
            "packageName" to uc.packageName,
            "returnType" to (uc.returnType ?: ""),
            "intentName" to intentName,
            "handlerName" to handlerName,
            "parameters" to uc.parameters.map { p ->
                mapOf(
                    "name" to p.name,
                    "kotlinType" to resolveParamTypeString(p.type, modelPackage),
                )
            },
        )
    }

    val stateFields = buildList {
        useCases.forEach { uc ->
            val rt = uc.returnType ?: return@forEach
            if (rt.contains("Flow", ignoreCase = true)) return@forEach
            val propName = uc.camelName
            val typeStr = resolveStatePropertyTypeString(rt, modelPackage)
            add(
                mapOf(
                    "name" to propName,
                    "typeFqn" to typeStr,
                    "nullable" to true,
                ),
            )
        }
    }

    val intentInners = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        val emptyParams = uc.parameters.isEmpty()
        mapOf(
            "simpleName" to intentName,
            "emptyParams" to emptyParams,
            "params" to uc.parameters.map { p ->
                mapOf(
                    "name" to p.name,
                    "kotlinType" to resolveParamTypeString(p.type, modelPackage),
                )
            },
        )
    }

    val useCaseHandlers = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        val rt = uc.returnType.orEmpty()
        val flowBased = rt.contains("Flow", ignoreCase = true)
        val resultBased = !flowBased && looksLikeResultReturn(rt)
        mapOf(
            "intentSimpleName" to intentName,
            "handlerName" to "handle${intentName.replaceFirstChar { it.uppercase() }}",
            "useCaseCamel" to uc.camelName,
            "hasParams" to uc.parameters.isNotEmpty(),
            "paramPassArgs" to uc.parameters.joinToString(", ") { "${it.name} = ${it.name}" },
            "showLoading" to !rt.contains("SseEmitter"),
            "resultBased" to resultBased,
            "flowBased" to flowBased,
        )
    }

    return mapOf(
        "pascalName" to pascalName,
        "camelName" to camelName,
        "modulePackage" to modulePackage,
        "screenPkg" to screenPkg,
        "modelPackage" to modelPackage,
        "resultPackage" to resultPackage,
        "useCases" to useCaseRows,
        "hasUseCases" to useCases.isNotEmpty(),
        "stateFields" to stateFields,
        "intentInners" to intentInners,
        "useCaseHandlers" to useCaseHandlers,
    )
}

private fun looksLikeResultReturn(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    return returnType.contains("Result<") ||
        returnType.contains(".Result<") ||
        returnType.endsWith(".Result") ||
        returnType.contains("network.domain.Result")
}

private fun resolveStatePropertyTypeString(returnType: String, modelPackage: String): String {
    val innerType = Regex("""Result<([^>]+)>""").find(returnType)?.groupValues?.get(1) ?: returnType
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
