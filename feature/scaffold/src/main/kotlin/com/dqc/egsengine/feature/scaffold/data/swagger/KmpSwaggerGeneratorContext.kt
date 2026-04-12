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

    val dataModelPackage = "$generateRootPackage.data.datasource.api.model"
    val servicePackage = "$generateRootPackage.data.datasource.api.service"
    val dataRepositoryPackage = "$generateRootPackage.data.repository"
    val domainModelPackage = "$generateRootPackage.domain.model"
    val domainRepositoryPackage = "$generateRootPackage.domain.repository"
    val domainUseCasePackage = "$generateRootPackage.domain.usecase"

    val serviceName = "${pascalModuleName}KtorfitService"
    val repositoryName = "${pascalModuleName}Repository"
    val repositoryImplName = "Generated${pascalModuleName}RepositorySupport"

    fun dataModelName(rawName: String): String = "${rawName.toSafePascal()}ApiModel"
    fun domainModelName(rawName: String): String = rawName.toSafePascal()

    fun hasResultWrappers(): Boolean =
        template.apiResultClass != null && template.commonResultClass != null && template.toResultPackage != null

    /**
     * Ktorfit + template network stack: `NetworkResult<CommonResult<T>, *>`.
     */
    fun serviceReturnType(responseType: SwaggerType?): String {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = false)
        if (!hasResultWrappers()) return bodyType
        val common = "${template.commonResultClass}<$bodyType>"
        return "${template.apiResultClass}<$common, *>"
    }

    fun repositoryReturnType(responseType: SwaggerType?): String {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = true)
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return "${template.baseClassPackages.resultClass}<$bodyType>"
        }
        return bodyType
    }

    fun resolveType(type: SwaggerType, forDomain: Boolean): String = when (type) {
        is SwaggerType.Primitive -> when (type.kind) {
            PrimitiveKind.STRING -> "kotlin.String"
            PrimitiveKind.INT -> "kotlin.Int"
            PrimitiveKind.LONG -> "kotlin.Long"
            PrimitiveKind.DOUBLE -> "kotlin.Double"
            PrimitiveKind.BOOLEAN -> "kotlin.Boolean"
        }

        is SwaggerType.ModelRef -> {
            val pkg = if (forDomain) domainModelPackage else dataModelPackage
            val simpleName = if (forDomain) domainModelName(type.name) else dataModelName(type.name)
            "$pkg.$simpleName"
        }

        is SwaggerType.ListType ->
            "kotlin.collections.List<${resolveType(type.elementType, forDomain)}>"

        is SwaggerType.MapType ->
            "kotlin.collections.Map<kotlin.String, ${resolveType(type.valueType, forDomain)}>"

        SwaggerType.Unknown -> "kotlinx.serialization.json.JsonElement"
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
