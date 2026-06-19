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

    fun hasResultWrappers(): Boolean = template.apiResultClass != null && template.commonResultClass != null && template.toResultPackage != null

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
     * When [SwaggerOperation.paging] is set, returns `Result<PageResult<Item>>` (or plain `PageResult<Item>`)
     * using [ModuleTemplate.baseClassPackages.pageResultClass].
     */
    fun repositoryReturnType(op: SwaggerOperation): String {
        val p = op.paging
        val bodyType = if (p != null) {
            val item = resolveType(p.itemType, forDomain = true)
            "${pageResultShortName()}<$item>"
        } else {
            resolveType(op.responseBody ?: SwaggerType.Unknown, forDomain = true)
        }
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return "Result<$bodyType>"
        }
        return bodyType
    }

    private fun pageResultShortName(): String = template.baseClassPackages.pageResultClass?.substringAfterLast('.') ?: "PageResult"

    fun resolveType(type: SwaggerType, forDomain: Boolean): String = SwaggerTypeMapping.resolveType(type, forDomain, ::dataModelName, ::domainModelName)

    fun importsForType(type: SwaggerType, forDomain: Boolean, currentPackage: String? = null): Set<String> = SwaggerTypeMapping.importsForType(type, forDomain, currentPackage, dataModelPackage, domainModelPackage, ::dataModelName, ::domainModelName)

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

    /** Imports when [SwaggerOperation.paging] drives `Result<PageResult<Item>>`. */
    fun importsForRepositoryReturnType(op: SwaggerOperation): Set<String> {
        val p = op.paging ?: return importsForRepositoryReturnType(op.responseBody)
        val inner = importsForType(p.itemType, forDomain = true, currentPackage = null).toMutableSet()
        template.baseClassPackages.pageResultClass?.let { inner.add(it) }
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            template.baseClassPackages.resultClass?.let { inner.add(it) }
        }
        return inner
    }

    /**
     * Maps an API page envelope (Ktorfit `it`) to domain [pageResultShortName] using list [PagingInfo.listPropertyName].
     */
    fun pagingRepositoryMapExpression(op: SwaggerOperation, sourceExpr: String): String {
        val p = op.paging ?: error("Expected paging for ${op.operationId}")
        val pr = pageResultShortName()
        val rowMapping = SwaggerTypeMapping.mapExpressionNonNull(p.itemType, "row", "toDomain")
        return "$pr(list = $sourceExpr.${p.listPropertyName}.map { row -> $rowMapping }, total = $sourceExpr.total, page = $sourceExpr.page, pageSize = $sourceExpr.pageSize, totalPages = $sourceExpr.totalPages)"
    }

    fun requiresToDomainImportForOperation(op: SwaggerOperation): Boolean {
        op.paging?.let { if (requiresToDomainImport(it.itemType)) return true }
        return requiresToDomainImport(op.responseBody)
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

    fun requiresToDomainImport(type: SwaggerType?): Boolean = SwaggerTypeMapping.requiresToDomainImport(type)

    fun toDataExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String = SwaggerTypeMapping.mapExpression(type, sourceExpr, nullableContainer, "toData")

    fun toDomainExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String = SwaggerTypeMapping.mapExpression(type, sourceExpr, nullableContainer, "toDomain")

    fun repositoryResponseMapExpression(type: SwaggerType?, sourceExpr: String): String? {
        val t = type ?: return null
        return when (t) {
            is SwaggerType.Primitive, SwaggerType.Unknown -> null
            is SwaggerType.ModelRef, is SwaggerType.ListType, is SwaggerType.MapType ->
                SwaggerTypeMapping.mapExpressionNonNull(t, sourceExpr, "toDomain")
        }
    }
}
