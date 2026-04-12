/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate

/**
 * Package and type resolution for KMP Swagger codegen (generate/ tree under commonMain).
 * Aligns with egs-kmp-template `feature/<m>/.../generate` layout.
 */
class KmpSwaggerGeneratorContext(val template: ModuleTemplate) {
    val moduleName = template.name
    val pascalModuleName = moduleName.toSafePascal()
    val rootPackage = template.packageName
    val generateRootPackage = "$rootPackage.generate"
    /** Koin modules for generated API (`GeneratedDataModule` / `GeneratedDomainModule`). */
    val generateDiPackage = "$generateRootPackage.di"

    val dataModelPackage = "$generateRootPackage.data.datasource.api.model"
    val servicePackage = "$generateRootPackage.data.datasource.api.service"
    val dataRepositoryPackage = "$generateRootPackage.data.repository"
    val domainModelPackage = "$generateRootPackage.domain.model"
    val domainRepositoryPackage = "$generateRootPackage.domain.repository"
    val domainUseCasePackage = "$generateRootPackage.domain.usecase"

    val serviceName = "${pascalModuleName}KtorfitService"

    /** Swagger/API slice: `TodoApiRepository`. */
    val apiRepositoryName = "${pascalModuleName}ApiRepository"

    /** `GeneratedTodoApiRepositorySupport` (Ktorfit delegation). */
    val apiRepositorySupportName = "Generated${pascalModuleName}ApiRepositorySupport"

    /** Combined domain contract: `TodoRepository` (extends API/DB sub-interfaces). */
    val combinedRepositoryName = "${pascalModuleName}Repository"

    fun dataModelName(rawName: String): String = "${rawName.toSafePascal()}ApiModel"
    fun domainModelName(rawName: String): String = rawName.toSafePascal()

    fun hasResultWrappers(): Boolean =
        template.apiResultClass != null && template.commonResultClass != null && template.toResultPackage != null

    /**
     * Ktorfit + template network stack: `NetworkResult<CommonResult<T>, *>`.
     * Uses short names; add [importsForServiceReturnType] imports at file top.
     */
    fun serviceReturnType(responseType: SwaggerType?): String {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = false)
        if (!hasResultWrappers()) return bodyType
        return "NetworkResult<CommonResult<$bodyType>, *>"
    }

    /**
     * Domain `Result<T>` with short name; add [importsForRepositoryReturnType] imports.
     */
    fun repositoryReturnType(responseType: SwaggerType?): String {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = true)
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return "Result<$bodyType>"
        }
        return bodyType
    }

    /**
     * Short Kotlin type names (imports via [importsForType] / [importsForServiceReturnType]).
     */
    fun resolveType(type: SwaggerType, forDomain: Boolean): String = when (type) {
        is SwaggerType.Primitive -> when (type.kind) {
            PrimitiveKind.STRING -> "String"
            PrimitiveKind.INT -> "Int"
            PrimitiveKind.LONG -> "Long"
            PrimitiveKind.DOUBLE -> "Double"
            PrimitiveKind.BOOLEAN -> "Boolean"
        }

        is SwaggerType.ModelRef -> {
            if (forDomain) domainModelName(type.name) else dataModelName(type.name)
        }

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

    /** Imports for Ktorfit return type (NetworkResult / CommonResult + body). */
    fun importsForServiceReturnType(responseBody: SwaggerType?): Set<String> {
        val inner = importsForType(responseBody ?: SwaggerType.Unknown, forDomain = false, currentPackage = null)
        if (!hasResultWrappers()) return inner
        val net = mutableSetOf<String>()
        template.apiResultClass?.let { net.add(it) }
        template.commonResultClass?.let { net.add(it) }
        return net + inner
    }

    /** Imports for repository / use case return `Result<T>`. */
    fun importsForRepositoryReturnType(responseBody: SwaggerType?): Set<String> {
        val inner = importsForType(responseBody ?: SwaggerType.Unknown, forDomain = true, currentPackage = null)
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return inner + buildSet { template.baseClassPackages.resultClass?.let { add(it) } }
        }
        return inner
    }

    fun ktorfitMethodAnnotationImport(method: String): String = when (method.uppercase()) {
        "GET" -> "de.jensklingenberg.ktorfit.http.GET"
        "POST" -> "de.jensklingenberg.ktorfit.http.POST"
        "PUT" -> "de.jensklingenberg.ktorfit.http.PUT"
        "DELETE" -> "de.jensklingenberg.ktorfit.http.DELETE"
        "PATCH" -> "de.jensklingenberg.ktorfit.http.PATCH"
        else -> "de.jensklingenberg.ktorfit.http.GET"
    }

    fun ktorfitMethodAnnotationSimple(method: String): String = when (method.uppercase()) {
        "GET" -> "GET"
        "POST" -> "POST"
        "PUT" -> "PUT"
        "DELETE" -> "DELETE"
        "PATCH" -> "PATCH"
        else -> "GET"
    }

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
