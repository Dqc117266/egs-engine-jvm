/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

/**
 * Type and package resolution for Swagger codegen (Kotlin source strings, no KotlinPoet).
 *
 * @param generateLayout When true, packages are rooted at `[base].generate`, the Swagger API slice is
 * `[Module]ApiRepository`, and the Retrofit delegation class is `Generated[Module]ApiRepositorySupport`
 * (aligned with KMP `generate/` tree).
 */
open class SwaggerGeneratorContext(
    val template: ModuleTemplate,
    private val generateLayout: Boolean = false,
) {
    val moduleName = template.name
    val pascalModuleName = moduleName.toSafePascal()
    val rootPackage =
        if (generateLayout) "${template.packageName}.generate" else template.packageName
    val dataPackage = "$rootPackage.data"
    val domainPackage = "$rootPackage.domain"
    val dataModelPackage = "$dataPackage.datasource.api.model"
    val servicePackage = "$dataPackage.datasource.api.service"
    val dataRepositoryPackage = "$dataPackage.repository"
    val domainModelPackage = "$domainPackage.model"
    val domainRepositoryPackage = "$domainPackage.repository"
    val domainUseCasePackage = "$domainPackage.usecase"
    val serviceName = "${pascalModuleName}RetrofitService"
    /** Combined domain contract `TodoRepository` (extends API/DB/Prefs slices). Same as [KmpSwaggerGeneratorContext.combinedRepositoryName]. */
    val repositoryName = "${pascalModuleName}Repository"
    /** Swagger/API slice `TodoApiRepository`. */
    val apiRepositoryName = "${pascalModuleName}ApiRepository"
    /** `GeneratedTodoApiRepositorySupport` (Retrofit delegation). */
    val apiRepositorySupportName = "Generated${pascalModuleName}ApiRepositorySupport"
    val repositoryImplName =
        if (generateLayout) apiRepositorySupportName else "${pascalModuleName}RepositoryImpl"

    /** True when using split `XApiRepository` + combined `XRepository` (Android `generate/` tree). */
    val usesSplitRepositoryLayout: Boolean get() = generateLayout

    /** Koin modules for generated API (`GeneratedDataModule` / `GeneratedDomainModule`). Only used when [generateLayout]. */
    val generateDiPackage: String get() = if (generateLayout) "$rootPackage.di" else rootPackage

    fun dataModelName(rawName: String): String = "${rawName.toSafePascal()}ApiModel"
    fun domainModelName(rawName: String): String = rawName.toSafePascal()

    fun hasResultWrappers(): Boolean =
        template.apiResultClass != null && template.commonResultClass != null && template.toResultPackage != null

    fun serviceReturnType(responseType: SwaggerType?): String {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = false)
        if (!hasResultWrappers()) return bodyType
        val commonSimple = simpleNameFromFqn(template.commonResultClass!!)
        val apiSimple = simpleNameFromFqn(template.apiResultClass!!)
        return "$apiSimple<$commonSimple<$bodyType>>"
    }

    fun repositoryReturnType(responseType: SwaggerType?): String {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = true)
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return "${simpleNameFromFqn(template.baseClassPackages.resultClass!!)}<$bodyType>"
        }
        return bodyType
    }

    /**
     * Short Kotlin type names; pair with [importsForType] / [importsForServiceReturnType] /
     * [importsForRepositoryReturnType] at file top (aligned with [KmpSwaggerGeneratorContext]).
     */
    fun resolveType(type: SwaggerType, forDomain: Boolean): String = when (type) {
        is SwaggerType.Primitive -> when (type.kind) {
            PrimitiveKind.STRING -> "String"
            PrimitiveKind.INT -> "Int"
            PrimitiveKind.LONG -> "Long"
            PrimitiveKind.DOUBLE -> "Double"
            PrimitiveKind.BOOLEAN -> "Boolean"
        }

        is SwaggerType.ModelRef ->
            if (forDomain) domainModelName(type.name) else dataModelName(type.name)

        is SwaggerType.ListType ->
            "List<${resolveType(type.elementType, forDomain)}>"

        is SwaggerType.MapType ->
            "Map<String, ${resolveType(type.valueType, forDomain)}>"

        SwaggerType.Unknown -> "JsonElement"
    }

    /**
     * Import lines (FQNs) required for [resolveType] when used in a file in [currentPackage].
     * Same-package model refs omit imports.
     */
    fun importsForType(type: SwaggerType, forDomain: Boolean, currentPackage: String? = null): Set<String> =
        when (type) {
            is SwaggerType.Primitive -> emptySet()
            is SwaggerType.ModelRef -> {
                val pkg = if (forDomain) domainModelPackage else dataModelPackage
                val simple = if (forDomain) domainModelName(type.name) else dataModelName(type.name)
                if (currentPackage != null && pkg == currentPackage) {
                    emptySet()
                } else {
                    setOf("$pkg.$simple")
                }
            }
            is SwaggerType.ListType -> importsForType(type.elementType, forDomain, currentPackage)
            is SwaggerType.MapType -> importsForType(type.valueType, forDomain, currentPackage)
            SwaggerType.Unknown -> setOf("kotlinx.serialization.json.JsonElement")
        }

    /** Imports for Retrofit return type (API result wrappers + body). */
    fun importsForServiceReturnType(responseBody: SwaggerType?): Set<String> {
        val inner = importsForType(responseBody ?: SwaggerType.Unknown, forDomain = false, currentPackage = null)
        if (!hasResultWrappers()) return inner
        val net = mutableSetOf<String>()
        template.apiResultClass?.let { net.add(it) }
        template.commonResultClass?.let { net.add(it) }
        return net + inner
    }

    /** Imports for repository / use case return `Result<T>` (or unwrapped body). */
    fun importsForRepositoryReturnType(responseBody: SwaggerType?): Set<String> {
        val inner = importsForType(responseBody ?: SwaggerType.Unknown, forDomain = true, currentPackage = null)
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return inner + buildSet { template.baseClassPackages.resultClass?.let { add(it) } }
        }
        return inner
    }

    private fun simpleNameFromFqn(fqn: String): String = fqn.substringAfterLast('.')

    fun retrofitMethodAnnotationImport(method: String): String = when (method.uppercase()) {
        "GET" -> "retrofit2.http.GET"
        "POST" -> "retrofit2.http.POST"
        "PUT" -> "retrofit2.http.PUT"
        "DELETE" -> "retrofit2.http.DELETE"
        "PATCH" -> "retrofit2.http.PATCH"
        else -> "retrofit2.http.GET"
    }

    fun retrofitMethodAnnotationSimple(method: String): String = when (method.uppercase()) {
        "GET" -> "GET"
        "POST" -> "POST"
        "PUT" -> "PUT"
        "DELETE" -> "DELETE"
        "PATCH" -> "PATCH"
        else -> "GET"
    }

    fun paramLocationAnnotationImport(location: String): String =
        if (location.lowercase() == "path") "retrofit2.http.Path" else "retrofit2.http.Query"

    fun toDomainExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String =
        mapExpression(type, sourceExpr, nullableContainer, "toDomain")

    fun repositoryResponseMapExpression(type: SwaggerType?, sourceExpr: String): String? {
        val t = type ?: return null
        return when (t) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> null

            is SwaggerType.ModelRef,
            is SwaggerType.ListType,
            is SwaggerType.MapType,
            -> mapExpressionNonNull(t, sourceExpr, "toDomain")
        }
    }

    fun requiresToDomainImport(type: SwaggerType?): Boolean {
        val t = type ?: return false
        return when (t) {
            is SwaggerType.ModelRef -> true
            is SwaggerType.ListType -> requiresToDomainImport(t.elementType)
            is SwaggerType.MapType -> requiresToDomainImport(t.valueType)
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> false
        }
    }

    fun toDataExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String =
        mapExpression(type, sourceExpr, nullableContainer, "toData")

    private fun mapExpressionNonNull(type: SwaggerType, sourceExpr: String, method: String): String =
        when (type) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> sourceExpr

            is SwaggerType.ModelRef -> "$sourceExpr.$method()"
            is SwaggerType.ListType -> "$sourceExpr.map { ${
                mapExpressionNonNull(
                    type.elementType,
                    "it",
                    method,
                )
            } }"

            is SwaggerType.MapType ->
                "$sourceExpr.mapValues { (_, value) -> ${
                    mapExpressionNonNull(
                        type.valueType,
                        "value",
                        method,
                    )
                } }"
        }

    private fun mapExpression(
        type: SwaggerType,
        sourceExpr: String,
        nullableContainer: Boolean,
        method: String,
    ): String {
        val mappedExpr = mapExpressionNonNull(type, sourceExpr, method)
        if (!nullableContainer) return mappedExpr
        return when (type) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> sourceExpr

            is SwaggerType.ModelRef -> "$sourceExpr?.$method()"
            is SwaggerType.ListType -> "$sourceExpr?.map { ${
                mapExpressionNonNull(
                    type.elementType,
                    "it",
                    method,
                )
            } }"

            is SwaggerType.MapType ->
                "$sourceExpr?.mapValues { (_, value) -> ${
                    mapExpressionNonNull(
                        type.valueType,
                        "value",
                        method,
                    )
                } }"
        }
    }
}

internal fun String.toSafePascal(): String =
    replace(Regex("[^A-Za-z0-9]"), " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString("") { part -> part.replaceFirstChar { c -> c.uppercase() } }
        .ifBlank { "AutoGen" }

internal fun String.toSafeIdentifier(): String {
    val id = replace(Regex("[^A-Za-z0-9_]"), "_")
    val headSafe = if (id.firstOrNull()?.isDigit() == true) "_$id" else id
    return if (headSafe in setOf(
            "in",
            "class",
            "object",
            "when",
            "is",
            "fun",
        )
    ) {
        "${headSafe}Value"
    } else {
        headSafe
    }
}
