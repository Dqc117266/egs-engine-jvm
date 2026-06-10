/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.common

import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo

/** Shared type-resolution helpers used by both Android and KMP page template mappers. */

internal fun shortenKotlinStdlibPrimitiveFqns(typeStr: String): String {
    var s = typeStr
    val nonChar = Regex("""kotlin\.(Long|Int|String|Boolean|Double|Float|Byte|Short)(\?)?""")
    s = nonChar.replace(s) { m -> m.groupValues[1] + m.groupValues[2] }
    val charOnly = Regex("""kotlin\.Char(\?)?(?![a-zA-Z])""")
    s = charOnly.replace(s) { m -> "Char" + m.groupValues[1] }
    return s
}

internal fun resolveParamTypeString(
    typeStr: String,
    modelPackage: String,
    modulePackage: String,
): String {
    val normalized = shortenKotlinStdlibPrimitiveFqns(typeStr.trim())
    val nullable = normalized.endsWith("?")
    val base = normalized.removeSuffix("?")
    val typeName =
        when {
            base.startsWith("List<") -> {
                val inner =
                    extractFirstGenericArgument(base, "List<")
                        ?: Regex("""List<([^>]+)>""").find(base)?.groupValues?.get(1)
                        ?: return "List"
                val innerType = resolveParamTypeString(inner, modelPackage, modulePackage)
                "List<$innerType>"
            }
            else ->
                base.split(".").let { parts ->
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
                                val candidate =
                                    when {
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

internal fun resolveBasicTypeString(
    simpleType: String,
    typePackage: String,
    modelPackage: String,
    modulePackage: String = "",
    fixEntity: Boolean = true,
): String {
    val cleaned = shortenKotlinStdlibPrimitiveFqns(simpleType)
    if (cleaned.startsWith("List<") && cleaned.endsWith(">")) {
        val innerType =
            extractFirstGenericArgument(cleaned, "List<")
                ?: cleaned.substring(5, cleaned.length - 1)
        val innerSimple = innerType.substringAfterLast(".")
        val innerPkg = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
        val inner = resolveBasicTypeString(innerSimple, innerPkg, modelPackage, modulePackage, fixEntity)
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
            val raw =
                if (cleaned.endsWith("ApiModel")) {
                    "$modelPackage.${cleaned.removeSuffix("ApiModel")}"
                } else {
                    "$typePackage.$cleaned"
                }
            if (fixEntity && modulePackage.isNotEmpty()) {
                fixRoomEntityFqn(normalizeSwaggerModelFqn(raw, modulePackage), cleaned, modulePackage)
            } else {
                raw
            }
        }
    }
}

internal fun databaseEntityPackage(modulePackage: String): String = "$modulePackage.generate.data.datasource.database.entity"

internal fun fixRoomEntityFqn(
    fqn: String,
    simple: String,
    modulePackage: String,
): String {
    if (!simple.endsWith("Entity")) return fqn
    val entityPkg = databaseEntityPackage(modulePackage)
    val wrong = "$modulePackage.generate.domain.model.$simple"
    if (fqn == wrong) return "$entityPkg.$simple"
    return fqn
}

internal fun normalizeSwaggerModelFqn(
    fqn: String,
    modulePackage: String,
): String {
    val legacy = "$modulePackage.domain.model."
    if (fqn.startsWith(legacy)) {
        return fqn.replaceFirst(legacy, "$modulePackage.generate.domain.model.")
    }
    return fqn
}

internal fun extractResultInnerType(returnType: String): String? {
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

internal fun defaultLiteralForUseCaseParamType(type: String): String {
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

internal fun looksLikeFlowReturn(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    if (returnType.contains("kotlinx.coroutines.flow")) return true
    return FLOW_TYPE_REGEX.containsMatchIn(returnType)
}

private val FLOW_TYPE_REGEX =
    Regex("""\b(Flow|StateFlow|SharedFlow|MutableStateFlow|MutableSharedFlow)\s*<""")

internal fun looksLikeResultReturn(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    return returnType.contains("Result<") ||
        returnType.contains(".Result<") ||
        returnType.contains("domain.result.Result<") ||
        returnType.contains("generate.domain.result.Result<") ||
        returnType.contains("base.domain.result.Result<") ||
        returnType.endsWith(".Result") ||
        returnType.contains("network.domain.Result")
}

internal fun shouldEmitStateFieldForReturnType(
    returnType: String,
    extractResultInner: (String) -> String?,
): Boolean {
    if (returnType.isBlank()) return false
    if (looksLikeFlowReturn(returnType)) return false
    val norm = shortenKotlinStdlibPrimitiveFqns(returnType.trim())
    val effective =
        extractResultInner(norm)
            ?: Regex("""Result<([^>]+)>""")
                .find(norm)
                ?.groupValues
                ?.get(1)
                ?.trim()
            ?: norm
    val trimmed = effective.trimEnd('?')
    val simple = trimmed.substringAfterLast(".")
    if (simple == "Unit" || trimmed == "kotlin.Unit") return false
    return true
}

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

internal fun unitEchoStatePropertyNameFor(
    uc: UseCaseInfo,
    paramFqn: String,
): String {
    val base = uc.name.removeSuffix("UseCase")
    if (base.startsWith("Update") && !base.startsWith("UpdateAll")) {
        val simple = paramFqn.trimEnd('?').substringAfterLast(".").removeSuffix("Entity")
        if (simple.isNotEmpty() && paramFqn.trimEnd('?').substringAfterLast(".").endsWith("Entity")) {
            return "updated" + simple.replaceFirstChar { it.uppercase() }
        }
    }
    return uc.camelName
}

internal fun isDirectReturnToStateUseCase(
    uc: UseCaseInfo,
    modelPackage: String,
    modulePackage: String,
): Boolean {
    val rt = uc.returnType?.trim() ?: return false
    if (rt.isBlank()) return false
    if (looksLikeFlowReturn(rt)) return false
    if (looksLikeResultReturn(rt)) return false
    if (isUnitParamEchoUseCase(uc, modelPackage, modulePackage)) return false
    if (!shouldEmitStateFieldForReturnType(rt) { extractResultInnerType(it) }) return false
    return true
}

internal fun resolveStatePropertyTypeString(
    returnType: String,
    modelPackage: String,
    modulePackage: String,
    fixEntity: Boolean = true,
): String {
    val returnTypeNorm = shortenKotlinStdlibPrimitiveFqns(returnType.trim())
    val innerType =
        extractResultInnerType(returnTypeNorm)
            ?: Regex("""Result<([^>]+)>""")
                .find(returnTypeNorm)
                ?.groupValues
                ?.get(1)
                ?.trim()
            ?: returnTypeNorm
    val simpleType = innerType.substringAfterLast(".")
    val typePackage = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
    return resolveBasicTypeString(simpleType, typePackage, modelPackage, modulePackage, fixEntity)
}

internal fun buildPagedUseCaseArgumentList(
    uc: UseCaseInfo,
    pageParam: String,
    pageSizeParam: String,
): String = uc.parameters.joinToString(",\n            ") { p ->
    val value =
        when (p.name) {
            pageParam -> "page"
            pageSizeParam -> "size"
            else -> defaultLiteralForUseCaseParamType(p.type)
        }
    "${p.name} = $value"
}

internal fun resolvePagedItemFqn(
    innerResultType: String,
    modelPackage: String,
    modulePackage: String,
    extractResultInner: (String) -> String?,
): String {
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

internal fun extractFirstGenericArgument(
    s: String,
    prefix: String,
): String? {
    if (!s.startsWith(prefix) || !s.endsWith(">")) return null
    val start = prefix.length
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

internal fun splitTopLevelCommaGenericArgs(args: String): List<String> {
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
        } else {
            when (c) {
                '<' -> depth++
                '>' -> depth--
            }
        }
        i++
    }
    return out
}
