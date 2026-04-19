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
 * Package layout: [modulePackage].presentation.screen.[camelPageName] (sources under `presentation/screen/`).
 * Swagger-generated models resolve under [modulePackage].generate.domain.model; [Result] uses the shared core-network type.
 */
internal fun PageTemplate.toKmpPageTemplateMap(): Map<String, Any?> {
    val pascalName = pageName
    val camelName = pascalName.replaceFirstChar { it.lowercase() }
    val modelPackage = "$modulePackage.generate.domain.model"
    val screenPkg = "$modulePackage.presentation.screen.$camelName"
    val resultClassFqn =
        baseClassPackages.resultClass ?: "template.core.base.network.domain.Result"

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
                    "kotlinType" to resolveParamTypeString(p.type, modelPackage, modulePackage),
                )
            },
        )
    }

    val stateFields = buildList {
        useCases.forEach { uc ->
            val rt = uc.returnType ?: return@forEach
            if (!shouldEmitStateFieldForReturnType(rt)) return@forEach
            val propName = uc.camelName
            val typeStr = resolveStatePropertyTypeString(rt, modelPackage)
            add(
                mapOf(
                    "name" to propName,
                    "typeFqn" to typeStr,
                    "typeContractRef" to contractShortTypeDisplay(typeStr),
                    "nullable" to true,
                ),
            )
        }
        useCases.forEach { uc ->
            if (!isUnitUpdateEntityEchoUseCase(uc, modelPackage, modulePackage)) return@forEach
            val entityFqn =
                resolveParamTypeString(uc.parameters.single().type, modelPackage, modulePackage)
            val propName = updatedStatePropertyNameForEntityFqn(entityFqn)
            add(
                mapOf(
                    "name" to propName,
                    "typeFqn" to entityFqn,
                    "typeContractRef" to contractShortTypeDisplay(entityFqn),
                    "nullable" to true,
                ),
            )
        }
    }

    val contractImports = buildContractImports(stateFields)

    val intentInners = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        val emptyParams = uc.parameters.isEmpty()
        mapOf(
            "simpleName" to intentName,
            "emptyParams" to emptyParams,
            "params" to uc.parameters.map { p ->
                mapOf(
                    "name" to p.name,
                    "kotlinType" to resolveParamTypeString(p.type, modelPackage, modulePackage),
                )
            },
        )
    }

    val useCaseHandlers = useCases.map { uc ->
        val intentName = uc.name.removeSuffix("UseCase")
        val rt = uc.returnType.orEmpty()
        val flowBased = looksLikeFlowReturn(rt)
        val unitEcho = isUnitUpdateEntityEchoUseCase(uc, modelPackage, modulePackage)
        val resultBased = !unitEcho && !flowBased && looksLikeResultReturn(rt)
        val echoProp =
            if (unitEcho) {
                updatedStatePropertyNameForEntityFqn(
                    resolveParamTypeString(uc.parameters.single().type, modelPackage, modulePackage),
                )
            } else {
                ""
            }
        mapOf(
            "intentSimpleName" to intentName,
            "handlerName" to "handle${intentName.replaceFirstChar { it.uppercase() }}",
            "useCaseCamel" to uc.camelName,
            "hasParams" to uc.parameters.isNotEmpty(),
            "paramPassArgs" to uc.parameters.joinToString(", ") { "${it.name} = ${it.name}" },
            "showLoading" to !rt.contains("SseEmitter"),
            "resultBased" to resultBased,
            "flowBased" to (!unitEcho && flowBased),
            "unitEntityEchoToState" to unitEcho,
            "unitEchoStatePropertyName" to echoProp,
            "unitEchoParamName" to if (unitEcho) "entity" else "",
        )
    }

    return mapOf(
        "pascalName" to pascalName,
        "camelName" to camelName,
        "modulePackage" to modulePackage,
        "screenPkg" to screenPkg,
        "modelPackage" to modelPackage,
        "resultClassFqn" to resultClassFqn,
        "useCases" to useCaseRows,
        "hasUseCases" to useCases.isNotEmpty(),
        "stateFields" to stateFields,
        "contractImports" to contractImports,
        "intentInners" to intentInners,
        "useCaseHandlers" to useCaseHandlers,
    )
}

private fun looksLikeFlowReturn(returnType: String): Boolean {
    if (returnType.isBlank()) return false
    if (returnType.contains("kotlinx.coroutines.flow")) return true
    return FLOW_TYPE_INVOKE_REGEX.containsMatchIn(returnType)
}

private val FLOW_TYPE_INVOKE_REGEX =
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

private fun isUnitUpdateEntityEchoUseCase(
    uc: UseCaseInfo,
    modelPackage: String,
    modulePackage: String,
): Boolean {
    if (!uc.name.startsWith("Update") || !uc.name.endsWith("UseCase")) return false
    val rt = uc.returnType?.trim()
    if (!rt.isNullOrBlank() && rt != "Unit" && rt != "kotlin.Unit") return false
    if (uc.parameters.size != 1 || uc.parameters.single().name != "entity") return false
    val fqn = resolveParamTypeString(uc.parameters.single().type, modelPackage, modulePackage)
    return fqn.trimEnd('?').substringAfterLast(".").endsWith("Entity")
}

private fun updatedStatePropertyNameForEntityFqn(entityFqn: String): String {
    val simple = entityFqn.trimEnd('?').substringAfterLast(".").removeSuffix("Entity")
    require(simple.isNotEmpty()) { "expected *Entity type, got $entityFqn" }
    return "updated" + simple.replaceFirstChar { it.uppercase() }
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

private fun resolveStatePropertyTypeString(returnType: String, modelPackage: String): String {
    val returnTypeNorm = shortenKotlinStdlibPrimitiveFqns(returnType.trim())
    val innerType = extractResultInnerType(returnTypeNorm)
        ?: Regex("""Result<([^>]+)>""").find(returnTypeNorm)?.groupValues?.get(1)?.trim()
        ?: returnTypeNorm
    val simpleType = innerType.substringAfterLast(".")
    val typePackage = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
    return resolveBasicTypeString(simpleType, typePackage, modelPackage)
}

/**
 * Extracts the type argument of the first `Result<...>` in [returnType], supporting nested generics
 * (e.g. `Result<PageResult<Foo>>`).
 */
private fun extractResultInnerType(returnType: String): String? {
    val idx = returnType.indexOf("Result<")
    if (idx < 0) return null
    var start = idx + "Result<".length
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

/**
 * Short type for Contract [State] properties (imports supply FQN); mirrors [collectContractImportsForType] structure.
 */
private fun contractShortTypeDisplay(typeFqn: String): String {
    val trimmed = typeFqn.trimEnd('?')
    val nullable = typeFqn.endsWith("?")
    val base = contractShortTypeDisplayInner(trimmed)
    return if (nullable) "$base?" else base
}

private fun contractShortTypeDisplayInner(s: String): String {
    if (s.startsWith("List<") && s.endsWith(">")) {
        val inner = extractFirstGenericArgument(s, "List<") ?: return s
        return "List<${contractShortTypeDisplayInner(inner)}>"
    }
    val open = s.indexOf('<')
    if (open > 0 && s.endsWith(">")) {
        val outer = s.substring(0, open)
        val args = s.substring(open + 1, s.length - 1)
        val outerShort = outer.substringAfterLast(".")
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

private fun buildContractImports(stateFields: List<Map<String, Any?>>): List<String> {
    val out = mutableSetOf<String>()
    stateFields.forEach { f ->
        val typeFqn = f["typeFqn"] as? String ?: return@forEach
        collectContractImportsForType(typeFqn, out)
    }
    return out.sorted()
}

private fun collectContractImportsForType(typeFqn: String, out: MutableSet<String>) {
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

private fun resolveBasicTypeString(simpleType: String, typePackage: String, modelPackage: String): String {
    val cleaned = shortenKotlinStdlibPrimitiveFqns(simpleType)
    if (cleaned.startsWith("List<") && cleaned.endsWith(">")) {
        val innerType = extractFirstGenericArgument(cleaned, "List<")
            ?: cleaned.substring(5, cleaned.length - 1)
        val innerSimple = innerType.substringAfterLast(".")
        val innerPkg = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
        val inner = resolveBasicTypeString(innerSimple, innerPkg, modelPackage)
        return "List<$inner>"
    }
    return when (cleaned) {
        "Boolean", "ModelBoolean", "KotlinBoolean" -> "Boolean"
        "Int", "ModelInt", "KotlinInt" -> "Int"
        "Long", "ModelLong", "KotlinLong" -> "Long"
        "String", "ModelString", "KotlinString" -> "String"
        "Double", "ModelDouble", "KotlinDouble" -> "Double"
        "Float", "ModelFloat", "KotlinFloat" -> "Float"
        else ->
            if (cleaned.endsWith("ApiModel")) {
                "$modelPackage.${cleaned.removeSuffix("ApiModel")}"
            } else {
                "$typePackage.$cleaned"
            }
    }
}

private fun databaseEntityPackage(modulePackage: String): String =
    "$modulePackage.generate.data.datasource.database.entity"

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
                        when {
                            parts.size == 1 -> "$entityPkg.$simple"
                            pkg == modelPackage || pkg.endsWith(".generate.domain.model") -> "$entityPkg.$simple"
                            else -> "$pkg.$simple"
                        }
                    } else {
                        "$pkg.$simple"
                    }
            }
        }
    }
    return if (nullable) "$typeName?" else typeName
}
